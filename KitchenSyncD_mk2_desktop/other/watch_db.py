#!/usr/bin/env python3

import pyinotify
import collections

wm = pyinotify.WatchManager()
mask = pyinotify.IN_DELETE | pyinotify.IN_CREATE

class EventHandler(pyinotify.ProcessEvent):
    past = collections.deque(maxlen = 3)

    def fin(self):
        if self.past[0] == "access" and self.past[1] == "modify" and self.past[2] == "close_write":
            print("should trigger a sync.")

    def process_IN_ACCESS(self, event):
        if event.pathname == "db.sqlite":
             self.past.append("access")
             self.fin()

    def process_IN_MODIFY(self, event):
        if event.pathname == "db.sqlite":
            self.past.append("modify")
            self.fin()

    def process_IN_CLOSE_WRITE(self, event):
        if event.pathname == "db.sqlite":
            self.past.append("close_write")
            self.fin()

    def process_IN_ATTRIB(self, event):
        if event.pathname == "db.sqlite":
            self.past.append("attrib")
            self.fin()

handler = EventHandler()
notifier = pyinotify.Notifier(wm, handler)
wdd = wm.add_watch('/srv/http/baikal/Specific/db/', pyinotify.IN_ACCESS | pyinotify.IN_MODIFY | pyinotify.IN_CLOSE_WRITE | pyinotify.IN_ATTRIB)

notifier.loop()
