Reactive Music trades Minecraft's default music for something dynamic, reactive, and ever-changing. Including a collection of fantasy and celtic tracks to give an air of wonder, a whisper of the unknown, and the call of adventure.

Reactive Music is based off a music pack I originally made for the Ambience mod back in 1.12. Now rebuilt to be a standalone mod for modern minecraft.

Discord: https://discord.gg/vJ6BCjPR3R

# ⚠️ This is an alpha experimental fork of Reactive Music.

New features were added, and the codebase was _**heavily**_ refactored with a lot of changes to internals. And also a whole lot of Javadocs comments for convenience and hopefully, a quicker adoption and improvement of this refreshed codebase that will allow a new ecosystem of **mods made for other mods** using the powerful new tools and systems available to developers through Reactive Music's Songpack & Event system.

Keep reading for a quick overview of the changes and additions.

# Many core pieces are now abstracted.
The codebase of Reactive Music `v1.x.x` was monolithic in various places, making it difficult to alter the main flow of the code or add features. I have aimed to improve developer experience through the following changes:
- The built-in events were moved to the new plugin system, allowing logic to be worked on within a final plugin class - making it easier to expand on these features and add new ones.
- The core logic behind the songpack loading & selection systems have been extracted into various new classes, making it easier to reimplement utility methods or hook into their logic at various points in the flow.

# The audio system has been ripped apart and rebuilt as a Manager / Worker pattern.
The `PlayerThread` class was a single instance of an audio player - which did it's job very well. But also with heavy restriction. Now, audio players and threads are created through `PlayerManager` classes, which handle tick-based fading, support making external calls, and more importantly - allow *multiple audio streams to exist.* These new instances are fully configurable, and allow for a deeper dive into Reactive Music not only as an event based *music* system for Minecraft, but basically an event based *sound engine*.

Some ideas that I have personally planned using this new functionality:
- Right clicking mobs with an empty hand plays an audio dialogue.
- Various actions the player may randomly trigger self-talk dialogue.
- Adding more immersive sounds to various objects.
- Fire gets louder the more things that are on fire.
- ^ in the same way - more immersive water ambience near specific biomes.

# There is now an API entrypoint for developers.
Having a single entry point into Reactive Music's systems means it's easier to modify functionality, or hook into. This also makes developing new plugins for Reactive Music fairly straightforward - with the goal of making it easy to create new functionality around the songpack and event system.

⚠️ The API documentation is a work-in-progress. And by that, I mean at the time of writing this I haven't started it. If you are a plugin developer, I've tried to include thoroughly detailed javadocs for most of the core things you will need to understand which should be easily accessible in your IDE. 

# SongpackEventPlugin
The main addition to this version of Reactive Music is the powerful plugin system. Using a service loader pattern, Reactive Music can now import external classes which follow the structure of the new `SongpackEventPlugin` class. To see examples of how this system works, take a look at the code for the built-in events now found in `plugins/`

⚠️ I will of course do my best to document the new plugin system as soon as possible, just as with the API. I did try to include javadocs for the important classes and methods added for the plugin system as well.


