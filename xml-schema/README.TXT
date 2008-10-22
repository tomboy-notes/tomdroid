README.TXT

This folder holds an attempt at creating an XML schema for Tomboy. This first release targets Tomboy XML notes format version 0.3.

The original schema is the RELAX-NG Compact (.rnc) form and converted to RELAX-NG XML using http://www.thaiopensource.com/relaxng/trang.html.
* On Ubuntu, the package in the repositories do not work for Gutsy and Hardy. I used the Java version from the site.

Converted using: 
java -jar trang.jar xml-schema/tomboy-notes-0.3.rnc xml-schema/tomboy-notes-0.3.rng
