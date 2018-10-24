# 5. The Authoring Workflow « The FC4 Methodology

Please note: this workflow is subject to change. We know it’s overly complex, and we hope to streamline it soon.

----

Once a basic YAML file has been created with some initial contents, the basic authoring workflow is:

1. Create a new git branch in your local instance of [the diagram repository](repository.md)
1. In your text editor: either create a new diagram source file or open an existing diagram source file
1. In a terminal, in your `fc4` working dir, run `cd tool && ./fc4 wcb`
   1. This starts the tool in a mode wherein it will watch your clipboard for diagram source YAML and process (clean up) that YAML when it sees that it’s been changed.
1. In your text editor, add/revise elements and relationships, then select-all and cut the diagram source from your editor into your system clipboard.
   1. This will cause fc4-tool to process the contents of your clipboard.
1. Switch to [Structurizr Express](https://structurizr.com/help/express) (SE) » paste the source into the YAML textarea » press tab to blur the textarea
   1. SE will either render the diagram, or display a red error indicator in its toolbar
   2. If SE shows its red error indicator, click the indicator button to bring up a dialog listing the errors
1. Use SE to arrange the elements and edges as desired
1. Cut the diagram source from the SE YAML textarea into your system clipboard.
   1. This will cause fc4-tool to process the contents of your clipboard.
1. Paste the diagram source back into the SE YAML textarea so as to re-render the diagram, now that the elements have been “snapped” to a virtual grid.
1. Continue to cut and past the diagram source between your text editor and SE, using SE to preview and adjust the rendered diagram, while fc4-tool cleans up the diagram as you work.
1. When you’re ready to wrap up:
   1. Paste the diagram source into your text editor and save the YAML file.
   1. In your terminal, hit `ctrl-c` to stop `fc4 wcb`
   1. Run `./fc4 render <path-to-yaml-file>` to generate a `.png` file alongside the `.yaml` file
1. Use your git UI of choice to commit your changes.
1. Push your local changes to the origin repository regularly.
1. When you’re ready to submit your changes for review, open a
   [Merge Request](https://docs.gitlab.com/ee/user/project/merge_requests/index.html) or
   [Pull Request](https://help.github.com/articles/about-pull-requests/) to get your changes
   reviewed and then merged into master.

----

That’s pretty much it, right now. But we hope to streamline this workflow soon.

----

Please continue to [Publishing](publishing.md) or go back to [the top page](README.md).
