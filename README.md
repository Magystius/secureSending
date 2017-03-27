# IAA Hausarbeit
Umsetzung der Hausarbeit für das Fach "Internet Anwendungsarchitekturen"

_Hinweis:_ Ein kurzer Leitfaden findet sich am Ende der Installationsanleitung

## Autoren
Celina Runge, Lisa Cannon, Timo Kruth, Tim Dekarz

## Aufgabenstellung
Für die Bibliothek der Nordakademie soll ein neues Verwaltungssystem eingeführt werden, dass die
Verwaltung des Bücherbestands und die Verfolgung der Leihvorgänge ermöglicht.
Aufgabe der Hausarbeit ist die DV-technische Konzeption und anschließende Realisierung der
nachfolgend aufgezählten Anwendungsfälle im Rahmen einer neuen, webbasierten Anwendung.

## Requirements
Zum Inbetriebnehmen der Anwendung müssen zunächst die folgenden Programme installiert werden:

1. [Git](https://git-scm.com/download/win) installieren
2. [Node.js](https://nodejs.org/en/) installieren
3. [JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installieren
4. [Tomcat](http://tomcat.apache.org/download-80.cgi) installieren
5. [IntelliJ IDEA Ultimate](https://www.jetbrains.com/idea/download/) installieren

Checke die korrekte Installation mit `git --version`, `node -v` und `npm -v`
_Hinweis: Terminal öffnen (entweder unten in der Symbolleiste oder oben auf View<Tool Windows<Terminal)_

## Installation Abhängigkeiten
Die Abhängigkeiten fürs Backend werden von Maven verwaltet und sollten von IntelliJ
automatisch installiert werden. Bei Bedarf können diese per Rechtsklick auf die 'pom.xml'
manuell nachinstalliert werden.
Die Abhänigkeiten fürs Frontend müssen manuell installiert werden. Dazu müssen
nacheinander folgende Befehle per Kommandozeile ausgeführt werden, sind bower und gulp
bereits installiert, können die Schritte 2-6 ignoriert werden:

1. `npm install`,
2. `npm install -g bower`,
3. `npm install -g gulp`,
4. `npm install -g gulp-cli`,
5. `bower install`,
6. `gulp`

Erst dann sind alle Abhängigkeiten fürs Frontend installiert. evtl. Warnungen können ignoriert werden,
Fehler nicht ;)

## Konfiguration IntelliJ
Konfigure das Projekt in IntelliJ analog zum Beispielprojekt in der Vorlesung.
Als Wurzelverzeichnis für den Tomcat Server muss '/' angegeben werden.

## Datenbank
Die Anwendung nutzt eine **H2** Datenbank. Eventuell müssen dafür notwendige
Treiber in IntelliJ installiert werden.

**Die Anwendung setzt einen minimalen Datenbestand voraus!**
Dieser kann über das sich im Ordner 'db' befindliche SQL-Skript erstellt werden,
dazu einfach per Rechtsklick das Skript ausführen ('run').

Alternativ kann die bereits vorhandene db genutzt werden.

Zunächst muss der Pfad zur Datenbank in der 'spring.xml' angepasst werden!
Sicherstellen, dass die Zeile `<prop key="hibernate.hbm2ddl.auto">update</prop>` genauso aussieht.

## Hinweise
Zur Anmeldung am System stehen folgende, initiale Nutzer zur Verfügung:

**Standardnutzer -> Admin:**
admin.admin@nordakademie.de
Passwort1!

**Student -> nur Listenansicht möglich**
thomas.heinze@nordakademie.de
Passwort1!

Diese **Installationsanleitung**, das **Pflichtenheft** und weitere
Unterlagen befinden sich im 'doc'-Verzeichnis.

Die Anwendung verfügt über ein wahnsinnig lustiges Easteregg, welches
mittels Eingabe des "Konami-Codes" aktiviert werden kann.

Die Anwendung wurde mit den folgenden Browsern getestet:
**Mozilla Firefox** & **Google Chrome**
Bei anderen Browsern wird für evtl. Darstellungsfehler keine
Garantie übernommen
_Hinweis:_ Aufgrund eines Bugs funktionieren in Firefox die Dialoge
nicht. Zum Testen wird daher geraten Chrome zu nutzen!

## Development
Im folgenden finden sich Hinweise zum Development der Anwendung

### Gulp Tasks
Folgende Gulp Tasks stehen zur Entwicklung zur Verfügung:

1. `gulp copy` - kopiert alle bower pakete ins webapp verzeichnis
2. `gulp css` - erstellt eine produktionsreife app.min.css Datei im webapp Verzeichnis
3. `gulp js` - erstellt eine produktionsreife app.min.js Datei im webapp Verzeichnis
4. `gulp lint` - Statische JS Codeanalyse

Alle Tasks stehen ebenfalls als 'npm'-Tasks zur Verfügung.

# Basic Usage
## Anmeldung
Die App bietet für Studenten eine einfache Listenansicht aller Publikationen
sowie eine Übersicht der eigenen Ausleihvorgänge.

Administrationen und Mitarbeiter haben Zugriff auf alle Funktionen der eigentlichen Anwendung.

##Dashboard
Die Anwendung verfügt über ein zentrales Dashboard auf dem alle relevanten Informationen
zusammengefasst sind. Erweiterte Statistiken lassen sich über die 'Analytics' Seite aufrufen.
Die drei Buttons über dem Seitenmenü bieten die Möglichkeit jederzeit einen Überblick
über offene Mahnungen, überfällige Ausleihen sowie den freien Bestand zu erhalten.

##Bestand
Im Bereich 'Publikationen' kann der Bestand der Bibliothek verwaltet werden.
Eine eigene Administrationsebene bietet die Möglichkeit Schlagwörter und Kategorien
zu verwalten. Über die Detailansicht lassen sich neue Ausleihen anstoßen bzw. offene
anschauen

##Nutzer
Im Bereich 'Nutzer' können Nutzer verwaltet werden. Die Rolle der Nutzer
entscheidet über die Rechte in der Anwendung. Der volle Funktionsumfang ist
für Administratoren und Mitarbeiter freigeschaltet. Über die Detailansicht
können offene Ausleihen angezeigt bzw. neue eröffnet werden.

##Verleihwesen
Im Bereich 'Verleihwesen' können die Ausleihen verwaltet werden. Sowohl über die Listenansicht,
als auch in der Detailansicht können Ausleihen verlängert, geschlossen, als verloren markiert
und ein neues Mahnverfahren eröffnet bzw. eine neue Mahnung verschickt werden. Bitte beachten, dass
die einzelnen Aktionen in Abhängigkeit vom jeweiligen, aktuellen Status zur Verfügung stehen.
Eine eigene Administrationsebene bietet die Möglichkeit u.a. Verleihperioden zu ändern.

##Allgemeines
Die Anwendung verfügt über eine zentrale Suche.

Weitere Informationen finden sich auf der FAQ-Seite der Anwendung.
