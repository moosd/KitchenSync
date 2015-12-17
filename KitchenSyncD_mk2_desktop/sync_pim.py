#!/usr/bin/env python3

from subprocess import call
import os, sys
import tempfile
import shutil
import sqlite3
import hashlib
from pid import PidFile
import socket
import time

# functions
def hashfile(afile, hasher, blocksize=65536):
    buf = afile.read(blocksize)
    while len(buf) > 0:
        hasher.update(buf)
        buf = afile.read(blocksize)
    return hasher.digest()

def sshcom(a):
    call(["ssh", "-p", "5120", "-o", "UserKnownHostsFile=/dev/null", "-o", "StrictHostKeyChecking=no", "shell@%s" % sys.argv[1], a])

def setsyncprogress():
    sshcom("su -c \"touch /dev/syncinprogress\"")

def unsetsyncprogress():
    sshcom("su -c \"rm /dev/syncinprogress\"")

def rsync(a, b):
    call(["rsync", "-e", "ssh -p 5120 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no", "-azvc", a, b])

def rsynctos(a, b):
    rsync(a, "shell@%s:%s" % (sys.argv[1], b))

def rsyncfroms(a, b):
    rsync("shell@%s:%s" % (sys.argv[1], a), b)

def copyback():
#    setsyncprogress()
    rsynctos("import/", "/sdcard/www/baikal/Specific/db/")
#    rsynctos("db.sqlite.import.history", "/sdcard/www/baikal/Specific/db/db.sqlite.history")
    sshcom("am broadcast -a com.moosd.kitchensyncd.UPDATE_CALENDAR -n com.moosd.kitchensyncd/.CalendarChangeReceiver")
#    unsetsyncprogress()

def updatehistory(file, file_hist):
    # go through file, if not present in history, mark as new
    file_hist.execute("create table if not exists entries(tab text not null, filename TEXT NOT NULL, timestamp integer, deleted integer not null);")
    for t in tables:
        cursor = file.execute("select uri, lastmodified from %s" % t)
        for row in cursor:
            if file_hist.execute("select count(*) from entries where tab = ? and filename = ?", (t, row[0])).fetchone()[0] < 1:
                file_hist.execute("insert into entries(tab, filename, timestamp, deleted) values(?, ?, ?, 0)", (t, row[0], row[1]))
            else:
                file_hist.execute("update entries set timestamp=?, deleted=0 where tab=? and filename=?", (row[1], t, row[0]))
    # go through history, if not present in main db, mark as deleted
    cursor = file_hist.execute("select tab, filename from entries where deleted=0")
    for row in cursor:
#        print("test %s/%s" % (row[0], row[1]))
        if file.execute("select count(*) from %s where uri=?" % row[0], (row[1],)).fetchone()[0] < 1:
            file_hist.execute("update entries set deleted=1 where tab=? and filename=?", (row[0], row[1]))
#            print("deleted %s/%s" % (row[0], row[1]))
    pass

def addnewobjects(frm, to):
    for t in tables:
        qstr = ",".join(columns[t])
        qqstr = ",".join(["?" for f in columns[t]])
        qqqstr = ",".join([f+"=?" for f in columns[t]])
        cursor = frm.execute("select %s from %s" % (qstr, t))
        for row in cursor:
            if to.execute("select count(*) from %s where uri=?" % t, (row[0],)).fetchone()[0] < 1:
                to.execute("insert into %s (%s) values (%s)" % (t, qstr, qqstr), row)
            else:
                ts = to.execute("select %s from %s where uri=?" % (qstr, t), (row[0],)).fetchone()
                if row[1] > ts[1]:
                    to.execute("update %s set %s where uri='%s'" % (t, qqqstr, row[0]), row)
                else:
                    frm.execute("update %s set %s where uri='%s'" % (t, qqqstr, row[0]), ts)

def syncdeletionshistories(frm, to):
    # go through history file. If present in other history file, update self with older timestamp
    cursor = frm.execute("select filename, timestamp, deleted from entries")
    for row in cursor:
        otherrow = to.execute("select filename, timestamp, deleted from entries where filename=?", (row[0],)).fetchone()
        if otherrow is not None:
            if otherrow[1] > row[1]:
                if otherrow[2] is not row[2]:
                    frm.execute("update entries set deleted=?, timestamp=? where filename=?", (otherrow[2], otherrow[1], row[0]))
            else:
                if otherrow[2] == 1:
                    frm.execute("update entries set deleted=?, timestamp=? where filename=?", (1, otherrow[1], row[0]))

def syncdeletionsproper(file, file_hist):
    cursor = file_hist.execute("select tab, filename from entries where deleted=1")
    for row in cursor:
        file.execute("delete from %s where uri=?" % (row[0]), (row[1],))

def ctagupdate(file, file2):
    for t in ctagtables:
        cursor = file.execute("select id, ctag from %s" % t)
        for row in cursor:
            f = file2.execute("select ctag from %s where id=?" % t, (row[0],)).fetchone()
            if f is not None:
                ctag = f[0]
                if row[1] != ctag:
                    ctag = ctag + 1
                    file.execute("update %s set ctag=? where id=?" % t, (ctag, row[0]))
                    file2.execute("update %s set ctag=? where id=?" % t, (ctag, row[0]))


ctagtables = ["calendars", "addressbooks"]
tables = ("calendarobjects", "cards")
columns = { "calendarobjects": ["uri", "lastmodified", "calendardata", "calendarid", "etag", "size", "componenttype", "firstoccurence", "lastoccurence"], "cards": ["uri", "lastmodified", "addressbookid", "carddata"] }

# copy our files to a temp place
with PidFile(piddir='/tmp'):
    dirpath = tempfile.mkdtemp()
    curdir = os.getcwd()
    os.chdir(dirpath)

    os.mkdir("import")
    shutil.copy("/srv/http/baikal/Specific/db/db.sqlite", dirpath)
    shutil.copy("/srv/http/baikal/Specific/db/db.sqlite.history", dirpath)

    # copy their files to a temp place
    shutil.copy("db.sqlite", "import/db.sqlite")
    shutil.copy("db.sqlite.history", "import/db.sqlite.history")
    rsyncfroms("/sdcard/www/baikal/Specific/db/", "import/")
#    print(dirpath)
#    time.sleep(300)
#    sys.exit(0)
#    rsyncfroms("/sdcard/www/baikal/Specific/db/db.sqlite.history", "db.sqlite.import.history")

    # open databases
    db = sqlite3.connect('db.sqlite')
    db_history = sqlite3.connect('db.sqlite.history')
    db_import = sqlite3.connect('import/db.sqlite')
    db_import_history = sqlite3.connect('import/db.sqlite.history')

    # if ctags are equal, do nothing...
    diff = 0
    for t in ctagtables:
        cursor = db.execute("select id, ctag from %s" % t)
        for row in cursor:
            f = db_import.execute("select ctag from %s where id=?" % t, (row[0],)).fetchone()
            if f is not None:
                ctag = f[0]
                if row[1] != ctag:
                    diff = 1
                    #print((row[1], ctag))

    if diff == 0:
        #copyback()
        db.close()
        db_history.close()
        db_import.close()
        db_import_history.close()

        os.chdir(curdir)
        shutil.rmtree(dirpath)
        sys.exit(0)

    # first, update histories for both
    updatehistory(db, db_history)
    updatehistory(db_import, db_import_history)

    # then add new objects to mine, then theirs
    addnewobjects(db_import, db)
    addnewobjects(db, db_import)

    # then sync deletions
    syncdeletionshistories(db_import_history, db_history)
    syncdeletionshistories(db_history, db_import_history)

    syncdeletionsproper(db, db_history)
    syncdeletionsproper(db_import, db_import_history)

    # update ctags if not equal
    ctagupdate(db, db_import)
    #ctagupdate(db_import, db)

    # update histories once again
    updatehistory(db, db_history)
    updatehistory(db_import, db_import_history)

    db.commit()
    db_import.commit()
    db_history.commit()
    db_import_history.commit()

    db.close()
    db_history.close()
    db_import.close()
    db_import_history.close()

    # copy back
    copyback()
    shutil.copy("db.sqlite", "/srv/http/baikal/Specific/db/db.sqlite.backup")
    shutil.copy("db.sqlite", "/srv/http/baikal/Specific/db/db.sqlite")
    shutil.copy("db.sqlite.history", "/srv/http/baikal/Specific/db/db.sqlite.history")

    # notify thunderbird
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(("localhost", 3000))
        s.send("refresh\r\n".encode("utf-8"))
        data = s.recv(64)
        s.close()
    except e:
        print("exception notifying thunderbird...")
        print(e)

    # clean up
    #print(dirpath)
    os.chdir(curdir)
    shutil.rmtree(dirpath)
