### Getting started

Install some dependencies:

    sudo apt-get install python-sphinx
    sudo pip install sphinx_rtd_theme
    

Now you can build the docs:

    make
    
You should now be able to point your browser to the `_build` directory and view the generated html pages.
To automatically rebuild the docs whenever a file is updated, you can run `sphinx-autobuild` from the `docs` root directory:

    sphinx-autobuild . _build/html
    
This will also serve the docs over HTTP at `http://localhost:8000`
