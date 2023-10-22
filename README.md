# Story Forge

A Text Game Engine, Any Platform

## Installation

preparing...

## Getting Started

preparing...


## Features
- [x] Effortlessly develop your Discord app using the command-line interface, and deploy it seamlessly without the necessity for code modifications.
- [ ] Effortlessly manage game quests within Unreal Engine using Story Forge.



## What is Story Forge?
Story Forge serves as a high-level game quest controller.

When developing mission system int an asynchronous situation, consider the following code: 
```scala
Client.Collect {
  case event: SlashCommandEvent =>
  // Mission part 1 for A
  // Mission part 2 for B

  case event: StringSelectEvent =>
  // Mission part 3 for A
  // Mission part 1 for B

  case event: ReactionAddEvent =>
  // Mission part 2 for A
  // Mission part 3 for B
}
```
However, managing missions with this code can be challenging.

Enter Story Forge, which simplifies mission management like this:
```scala
missionA = {
  one = SlashCommandAndWait
  two = ReactionAddCommandAndWait(one)
  three = StringSelectCommandAndWait(two)
}
missionB = {
  one = StringSelectCommandAndWait
  two = SlashCommandAndWait(one)
  three = ReactionAddCommandAndWait(two)
}
```
With Story Forge, mission steps are clearly organized, providing a more manageable structure.


### Demo
- [Wadi D Operation | Trailer](https://www.youtube.com/watch?v=o6OABgB9C3w)

## Contributors

- [AloneDary](https://github.com/AloneDary)
- [0pg](https://github.com/0pg)

## License

[License](LICENSE)
