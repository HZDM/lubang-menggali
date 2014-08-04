Lubang Menggali
===============

This web application ships a Lubang Menggali game engine, where multiple users
are allowed to play simultaneously in couples.


Usage
=====

Real-time game updates are provided via WebSocket connections. Hence, you need
to use a web browser that supports WebSockets. The application is written using
Play Framework. You need to use Typesafe Activator to run and test the
application.

In the directory you download the sources, type

    $ activator run

to run the application. In order to run the unit and integration tests, type

    $ activator test

in console. Note that the integration tests require a Firefox browser installed
on the host machine.


Authors
=======

This software is written by Volkan Yazıcı <volkan.yazici@gmail.com> in response
to a technical assignment sent by bol.com recruiting team.


The Game
========

Details of the game are presented below.


Board Setup
-----------

Each of the two players has his six pits in front of him. To the right of the
six pits, each player has a larger pit, his Lubang Menggali. In each of the six
round pits are put six stones when the game starts.


Game Play
---------

The player who begins with the first move picks up all the stones in anyone of
his own six pits, and sows the stones on to the right, one in each of the
following pits, including his own Lubang Menggali. No stones are put in
the opponents' Lubang Menggali. If the player's last stone lands in his own
Lubang Menggali, he gets another turn. This can be repeated several times before
it's the other player's turn.


Capturing Stones
----------------

During the game the pits are emptied on both sides. Always when the last stone
lands in an own empty pit, the player captures his own stone and all stones in
the opposite pit (the other players' pit) and puts them in his own Lubang
Menggali.


The Game Ends
-------------

The game is over as soon as one of the sides run out of stones. The player who
still has stones in his pits keeps them and puts them in his/hers Lubang
Menggali. Winner of the game is the player who has the most stones in his Lubang
Menggali.
