# AfroAuction
A Bukkit plugin to turn chests into auction shops with bidding and timers. Built for Paper 1.20.4 with Java 17 and Maven.

## Features
- Create auctions with `/createauction <startPrice> <durationSeconds>`.
- Bid via GUI with Vault economy integration.
- Configurable settings in `config.yml`.

## Setup
1. Install Paper 1.20.4, Vault, and an economy plugin (e.g., EssentialsX).
2. Build with `mvn package`.
3. Copy `target/AfroAuction-1.0-SNAPSHOT.jar` to `plugins`.

## Usage
- Hold an item, look at a chest, and run `/createauction 100 300`.
- Right-click the chest to bid via GUI.
