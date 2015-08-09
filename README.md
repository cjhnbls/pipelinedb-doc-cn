### Getting started

Install some dependencies:

    sudo apt-get install python-sphinx
    sudo pip install sphinx_rtd_theme sphinx-autobuild awscli

Now you can build the docs:

    make

To serve the docs over HTTP at `http://localhost:8000`, run:

    make serve

This will also automatically rebuild the docs and restart the HTTP server whenever a file is updated.

### Publishing

These docs are automatically deployed to [docs.pipelinedb.com](http://docs.pipelinedb.com) whenever any changes are pushed to `master`. To publish the docs manually, you'll need to have the [AWS client](http://aws.amazon.com/cli/) installed and configured. Then, just run

    make s3

This will upload the docs to a public S3 bucket that serves as a static [website](http://pipelinedb-docs.s3-website-us-west-2.amazonaws.com/index.html) host.
