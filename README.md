# Scrolloflex

Scrolloflex is a flexible, self-contained, single-purpose "Rolodex" webapp for contact management.

[![scrolloflex screenshot](https://github.com/thebendavis/scrolloflex/raw/master/docs/scrolloflex-screenshot.png)](https://github.com/thebendavis/scrolloflex/raw/master/docs/scrolloflex-demo.gif)

Features:

* Simple web app interface accessible from any browser
* Fast editing, sorting, and incremental searching. Open any contact with a click. Save changes with one key press.
* Give phone numbers and email addresses arbitrary tags
* Nicknames for each contact. No more "John Smith" and "Smith, John" as different entries in your address book.
* The web app is self-contained in a single JAR file
* Contact data is stored locally in a flat file in the [edn](https://github.com/edn-format/edn) format
* Simple API for adding and searching contacts from the command line or clients like [Mutt](http://www.mutt.org/)
* Optional IP-based whitelisting


## Requirements

* Java
* [leiningen](http://leiningen.org/) for development


## Building

    git clone https://github.com/thebendavis/scrolloflex.git
    cd scrolloflex

Build the documentation (will be stored in `docs/uberdoc.html`):

    lein marg

Build the JAR file (will be written to `target/scrolloflex-*-standalone.jar`):

    lein ring uberjar


## Run

If you have downloaded or built the JAR, run like any other JAR:

    java -jar scrolloflex-*-standalone.jar

Alternatively, build and run from source with:

    lein ring server-headless

The web interface will start on [http://localhost:3000](http://localhost:3000).


## Configuration

By default, the server will listen on port 3000.
Change the port by setting the `PORT` environment variable:

    PORT=9001 java -jar target/scrolloflex-*-standalone.jar
    # or
    PORT=9001 lein ring server-headless

On launch, scrolloflex will look for the database file `scrolloflex.edn`, creating it if necessary.
To specify a different database file, use the `DBFILE` environment variable or set the `dbfile` option:

    java -Ddbfile="/home/user/data/contacts.edn" -jar scrolloflex-*-standalone.jar
    # or
    DBFILE="/home/user/data/contacts.edn" lein ring server-headless

Scrolloflex will serve requests from any IP by default, but restrict the app to serve requests from a whitelist of specific IPs by setting the `WHITELIST` environment variable to a comma-separated list of IPs. Don't forget localhost IPs, if needed.

    java -Dwhitelist="127.0.0.1,0:0:0:0:0:0:0:1,192.168.1.99" -jar scrolloflex-*-standalone.jar
    # or
    WHITELIST="127.0.0.1,0:0:0:0:0:0:0:1,192.168.1.99" lein ring server-headless


## Access from Mutt and the Command Line

To query email addresses from within Mutt, add the following to your `muttrc`:

    set query_command = "curl -s --data-urlencode 'q=%s' http://localhost:3000/mutt-query"

Contacts and email addresses can be added by POSTing to `/add-email` with `name` and `address` parameters, e.g.:

    curl --data-urlencode "name=John Smith" --data-urlencode "address=john.smith@example.com" http://localhost:3000/add-email

If scrolloflex has a contact with that name, but not that email address, the email will be added to that contact.
If the email address exists, but associated to a contact with a different name, the name will be added as a nickname to the existing contact (avoiding duplicate contacts of the form "John Smith" and "Smith, John", for example).
If both the contact name and email address are new, a new contact will be created.

To extract the sender address from an email in Mutt and send it scrolloflex, create a script like `scrolloflex-add.sh`:

    #!/bin/sh

    FROM_LINE=$(grep "^From:.*$" < /dev/stdin)
    NAME=$(echo "$FROM_LINE" | egrep -o "^[^<]+" | cut -c 6- | sed -e 's/^ *//g' -e 's/ *$//g')
    ADDRESS=$(echo "$FROM_LINE" | egrep -o "[^<]+@[^>]+")

    echo "adding to contacts: '$NAME' $ADDRESS"
    curl --data-urlencode "name=$NAME" --data-urlencode "address=$ADDRESS" http://localhost:3000/add-email

Then I add to my `muttrc`:

    macro index,pager A "<pipe-message>scrolloflex-add.sh<return>" "add the sender address to contacts"

Customize as necessary for your setting.


## Documentation

Annotated source code: [https://rawgit.com/thebendavis/scrolloflex/master/docs/uberdoc.html](https://rawgit.com/thebendavis/scrolloflex/master/docs/uberdoc.html)
