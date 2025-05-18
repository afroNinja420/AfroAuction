# AfroAuction

AfroAuction is a Minecraft plugin that allows players to create and participate in auctions using in-game chests. Players can auction items, bid on auctions via a GUI, manage their pending items, and view all active auctions. The plugin features holograms above auction chests, a bidding system with percentage-based increments, and notifications for outbids and auction results.

## Features

- **Chest-Based Auctions**: Start auctions by interacting with a chest while holding an item.
- **Holograms**: Displays auction details (item name, highest bid, time left) above the chest using armor stands and a floating item.
- **Bidding System**: Players can bid via a GUI, with a minimum bid increment (default 10%). Invalid bids prompt for a retry without reopening the GUI.
- **Auction GUI**: View auction details (starting bid, highest bid, bidder, time left) and place bids. Auction creators see a "Settings" button to configure auction options.
- **Active Auctions GUI**: View all active auctions in a dedicated GUI, showing each auction’s item with details (start price, highest bid, creator, time left).
- **Notifications**: Receive messages for outbids, auction wins, and auction completions.
- **Pending Items**: Items from ended auctions (won or unsold) are stored for later claiming with `/pa claim`.
- **Configurable Settings**: Customize auction rules, hologram display, and messages via `config.yml`.

## Commands

All commands start with `/pa` or `/playerauction`. The following are available:

- `/pa create <price> <duration>`  
  Starts a new auction for the item in your hand at the chest you're looking at.  
  - `<price>`: Starting bid (e.g., `100`). Must be between `min-start-price` and `max-start-price`.  
  - `<duration>`: Auction duration (e.g., `30s`, `1m`, `1h2m`). Format: `[days]d[hours]h[minutes]m[seconds]s`. Must be between `min-auction-duration` and `max-auction-duration`.  
  Example: `/pa create 100 30s`

- `/pa notifications`  
  Toggles auction notifications on or off (e.g., outbid messages).

- `/pa claim`  
  Claims all pending items (won auctions or unsold items) if your inventory has space.  
  Aliased as `/playerauction claim`.

- `/pa activeauctions`  
  Opens a GUI displaying all currently active auctions, showing each auction’s item and details.  
  Example: `/pa activeauctions`

- `/pa help`  
  Displays a list of all available commands with their syntax and descriptions in chat.  
  Example: `/pa help`

## Configuration

The `config.yml` file allows customization of the plugin’s behavior. Below are the key settings:

- `auction-cooldown`: Cooldown between auctions for a player (in seconds, default: `60`).
- `max-active-auctions`: Maximum number of active auctions per player (default: `5`).
- `min-start-price` / `max-start-price`: Minimum and maximum starting bid price (default: `1.0` / `1000000.0`).
- `min-auction-duration` / `max-auction-duration`: Minimum and maximum auction duration in seconds (default: `30` / `86400`).
- `min-bid-percentage-increment`: Minimum bid increment as a percentage of the current bid (default: `10`).
- `bid-button-item`: Material for the bid button in the GUI (default: `EMERALD`).
- `hologram-base-height`: Height above the chest for the hologram (default: `1.7`).
- `hologram-line-spacing`: Vertical spacing between hologram lines (default: `0.25`).
- `hologram-item-offset`: Height of the floating item above the top hologram line (default: `0.25`).
- `hologram-update-interval`: Hologram update interval in ticks (default: `20`).
- `messages`: Customizable messages with color codes (e.g., `&a` for green). See `config.yml` for all message keys.

## Installation

1. **Download the Plugin**:
   - Download the latest `AfroAuction-1.0-SNAPSHOT.jar` from the GitHub releases page or build it using Maven.

2. **Install Dependencies**:
   - Install [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin (e.g., EssentialsX) on your server.

3. **Add the Plugin**:
   - Place `AfroAuction-1.0-SNAPSHOT.jar` in your server’s `plugins/` folder.

4. **Start the Server**:
   - Start your server to generate the `plugins/AfroAuction/config.yml` file.

5. **Configure**:
   - Edit `config.yml` to adjust settings like auction duration, bid increments, and messages.

6. **Restart the Server**:
   - Restart your server to apply the configuration changes.

## Usage

1. **Start an Auction**:
   - Hold an item, look at a chest, and run `/pa create 100 30s` to start an auction with a $100 starting bid for 30 seconds.
   - A hologram will appear above the chest showing the item name, highest bid, and time left.

2. **Bid on an Auction**:
   - Right-click the chest to open the auction GUI.
   - Click the bid button (emerald in slot 15) to enter a bid.
   - Type your bid amount in chat (e.g., `110`) or `cancel` to abort. If your bid is too low, you’ll be prompted to try again.

3. **Claim Items**:
   - After an auction ends, use `/pa claim` to retrieve won or unsold items.

4. **View Active Auctions**:
   - Run `/pa activeauctions` to open a GUI listing all active auctions.
   - Each auction is represented by its item, with details in the lore.

## Dependencies

- **Vault**: Required for economy integration (e.g., for handling bids and payments).
- **Economy Plugin**: An economy plugin like EssentialsX that works with Vault.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
