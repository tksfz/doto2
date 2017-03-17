Doto - task planning for teams
==============================

Doto is a collaborative task planning system with the following features:
- Distributed, using git for storage and sync
- Hierarchical, representing work as an arbitrarily deep tree
- Support for more complex workflows using _threads_ and _events_

Building Doto
-------------

To build doto from source, clone this repo and use `sbt stage`. (You may need a recent version of sbt.) Doto is under active development and there is no binary distribution at the moment.


Experiment with the `dotodoto`[1] repo
-----------------------------------
Doto uses git as a backend. At the moment, git has not yet been integrated into doto, so you just run git yourself:

```
git clone https://github.com/tksfz/dotodoto.git
```

will clone `dotodoto` into a local directory called `dotodoto` as usual. cd into that directory, then run `doto ls` to see all tasks in the `dotodoto` repo.

Use `doto help` to see a list of all commands. The most useful commands are `doto add` to add a task or event, and `doto thread` to create a thread.

Doto in a nutshell
------------------

*Tasks.* Tasks are listed with a checkbox `[ ]`. Everyone knows what tasks are. That's all the work you do, the product you build, the designs you make, the documentation you write, the bugs you find and fix, etc. Tasks are _divisible_ into sub-tasks.

*Threads.* Threads (indicated with `~~`) are a grouping of tasks that all make progress toward the same goal. Threads are purely organizational. They don't themselves represent work to be done; they organize work into logical threads. Threads are useful in a variety of situations: for separating feature-building work from maintenance work; for distinguishing work on the "critical path" vs work that's off that path; etc.

Threads can have sub-threads, in addition to having tasks. Everything in a doto repo lives under a root thread.

*Events.* Events (`![ ]`) map roughly to product code deployments. Events live within event threads (`~~!`), which lay out a sequence of deployments. The planning process in doto involves attaching tasks to events. That is, saying what will be deployed when, with an understanding of what effects should occur as a result. By compelling users to specify up-front what get deployed in what order, doto encourages early and frequent deployments, and iterative development.

`dotodoto`[1] is the doto repo for doto development.

[1]: https://github.com/tksfz/dotodoto
