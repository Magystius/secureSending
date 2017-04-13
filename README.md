# Secure Sending
This repository contains an application for providing an easy way to send personal, protect-worthy
information to customers or prospective clients. 

Its a technical use case study to develop a proof-of-concept prototyp for this kind of application.
Therefore see this app as what it is - just a prototyp and not ready for production - but feel free
to adapt and expand this app for your own needs (see *LICENSE, TECHNICAL DETAILS AND NOTES*).

## Authors
Tim Dekarz

## Key functions and Context
Companies have do deal with customers who want to recieve personal information via e-mail on a daily 
 basis. Wether its the latest invoice or - even more critical - information about private health insurances of 
 financial details.
 
 This use case study tries to solve this problem with an easy to use webbased application. 
 
 **Key Functions:**
 * Webform for entering customer details.
 * WYSIWYG-Editor to write a personal cover letter on-the-fly
 * Embed images directly into the letter - e.g. screenshots, logos
 * Upload multiple files as appendix (docx, xlsx, pptx, jpg, png -> get converted to pdf)
 * Generate a pdf-file from form inputs
 * Preview function
 * Merge all files into a single pdf
 * Send a e-mail with a personal link to the customer *TODO*
 * Provide secure acces to remote files via previously set one-time password *TODO*

## Requirements
For proper usage and installation please make sure you have the following installled first:

1. install [Git](https://git-scm.com/download/win)
2. install [Node.js](https://nodejs.org/en/)
3. install [JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
5. install [IntelliJ IDEA Ultimate](https://www.jetbrains.com/idea/download/) 

_TODO_
**Note:** A prepacked, ready to run app is located in the dist directory

## Install Depedencies
IntelliJ - or any other IDE - should import the maven dependencies automaticly.
Maybe you need to install bower first before import the frontend dependencies, otherwise just
run the install command.

1. (`npm install -g bower`),
2. `bower install`,

**NOTE:** As WYSIWYG-Editor 'tinymce' is used. please have a look at their docs, 
if you need a localization! 

## Technical Details
Please take a look at the javadoc and inline comments!

Main purpose of this project for me was - besides fullfilling the requirements - testing some
libraries ;-) Therefore, credits goes to:
- Spring Boot
- Lombok
- iText
- Documents4j
- jQuery
- Materialize
- tinymce

Visit their sites and docs for further information.

Altthough their isnt too much source code take a quick look at some interesting features:

As you would guess the validationService validates all the user input data. As this is a prototyp
its pretty basic and you probably would like to add some more features here. As it goes for the rest
of the application, security wasnt a main concern. so neither the files nor the html-string are checked
in any way. And, as for now, the uploaded and generated files, including embedded images lay temporarily
on the server and are accessible without any further authentifaction.

Speaking of files. Currently all generated pdfs and images are written as persistent files on your disk.
You have to considerate between in-memory handling with possibly huge ram usage - depends on 
the concurrent number of users - and many I/O-Operations. Your choice ;)

For the generation and merging of the pdf-files the itext library is used. It works satisfactory,
but you may need to change this depending on your requirements.
Same with the converting. I`ve tried some libraries and none of them were providing the results I wanted.
So I came up with documents4j. A library which basically runs a script to call the native ms-office function.
This approach obviously brings some disadvantages with it. You need a copy of ms-office on your
maschine and its only runnable on windows. Their is a remote-call feature, but I havent tried it out
yet. **NOTE:** _Please keep an eye one the MS-Office EULA and their restrictions of usage with other programs_

## TODOS
* Add Mailing Service for sending a mail with personal link to customer
* Provide one-time access to files 
* Authentification of customer with password
* Encrypt the pdf ?
