Doto - task planning for teams
==============================

Doto is a collaborative task planner with the following features:
- Distributed, using git for storage and sync
- Hierarchical, representing work as an arbitrarily deep tree
- Support for more complex workflows using _threads_ and _events_

Building Doto
-------------

To build doto from source, clone this repo and use `sbt stage`. (You may need a recent version of sbt.) This produces a binary at `target/universal/stage/bin/doto`. Put that in your PATH. Doto is under active development and there is no binary distribution at the moment.


Experiment with the `dotodoto` repo
-----------------------------------
`dotodoto`[1] is the doto repo for doto development. Doto uses git for storage and syncing. To get `dotodoto` do:

```
doto clone https://github.com/tksfz/dotodoto.git
```

will clone `dotodoto` into a directory underneath your DOTO_HOME (`~/.doto`) and set it as your active project. Unlike git, doto doesn't detect the active project based on your current directory. Instead the `doto project` command lets you set the active project.

Now run `doto ls` to see all tasks in the `dotodoto` repo.

Use `doto help` to see a list of all commands. The most useful commands are `doto add` to add a task or event, and `doto thread` to create a thread.

Doto in a nutshell
------------------

*Tasks.* Tasks are listed with a checkbox `[ ]`. Everyone knows what tasks are. That's all the work you do, the product you build, the designs you make, the documentation you write, the bugs you find and fix, etc. Tasks are _divisible_ into sub-tasks.

*Threads.* Threads (indicated with `~~`) are a grouping of tasks that all make progress toward the same goal. Threads are purely organizational. They don't themselves represent work to be done; they organize work into logical threads. Threads are useful in a variety of situations: for separating feature-building work from maintenance work; for distinguishing work on the "critical path" vs work that's off that path; etc.

Threads can have sub-threads, in addition to having tasks. Everything in a doto repo lives under a root thread.

*Events.* Events (`![ ]`) map roughly to product code deployments. Events live within event threads (`~~!`), which lay out a sequence of deployments. The planning process in doto involves attaching tasks to events. That is, saying what will be deployed when, with an understanding of what effects should occur as a result. By compelling users to specify up-front what gets deployed in what order, doto encourages early and frequent deployments, and iterative development.

Creating and pushing a new doto project
---------------------------------------

To create a new doto project, use `doto new`. To push this project to a central git repo, you'll need to:
- Create the repo on, say, github.com
- Run the usual `git remote`, `git add`, and `git push` commands manually, from the doto git repo in ``~/.doto/<project name>`

[1]: https://github.com/tksfz/dotodoto
