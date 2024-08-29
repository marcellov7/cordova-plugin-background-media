# cordova-plugin-media-session

Cordova plugin for Media Sessions on iOS and Android. This plugin enables:
- Customizable media playback notifications (including controls) on Android
- Media control using hardware media keys (e.g. on headsets, remote controls, etc.)
- Setting media metadata that can be used by the platform UI

## Installation

```bash
cordova plugin add cordova-plugin-media-session
```

## Usage

The API of this plugin is modeled after the Media Session Web API. Here's how to use it:

```javascript
var mediaSession = cordova.plugins.MediaSession;

// Set metadata
mediaSession.setMetadata({
    title: 'Song Title',
    artist: 'Artist Name',
    album: 'Album Name',
    artwork: [{ src: 'path/to/artwork.png', sizes: '512x512', type: 'image/png' }]
});

// Set playback state
mediaSession.setPlaybackState({ playbackState: 'playing' });

// Set action handlers
mediaSession.setActionHandler({ action: 'play' }, function() {
    // Handle play action
});

mediaSession.setActionHandler({ action: 'pause' }, function() {
    // Handle pause action
});

// Set position state
mediaSession.setPositionState({
    duration: 300,
    playbackRate: 1,
    position: 150
});
```

## API Reference

### setMetadata(options)

Sets the metadata for the currently playing media.

- `options`: An object containing metadata properties:
  - `title`: The title of the media (string)
  - `artist`: The artist name (string)
  - `album`: The album name (string)
  - `artwork`: An array of artwork objects, each containing:
    - `src`: URL of the artwork image
    - `sizes`: Size of the image (e.g., '512x512')
    - `type`: MIME type of the image (e.g., 'image/png')

### setPlaybackState(options)

Sets the current playback state.

- `options`: An object containing:
  - `playbackState`: A string representing the playback state ('playing', 'paused', or 'none')

### setActionHandler(options, handler)

Sets a handler for a specific media action.

- `options`: An object containing:
  - `action`: A string representing the action ('play', 'pause', 'seekbackward', 'seekforward', 'previoustrack', 'nexttrack', 'stop', 'seekto')
- `handler`: A function to be called when the action is triggered

### setPositionState(options)

Updates the current media playback position state.

- `options`: An object containing:
  - `duration`: The total duration of the media in seconds (number)
  - `playbackRate`: The current playback rate (number)
  - `position`: The current playback position in seconds (number)

## Platform Specifics

- On iOS that support it, this plugin uses the standard Web Media Session API.
- On Android, where the Web Media Session API is not available in WebView, this plugin provides a native implementation that mimics the behavior of the Web API.

## Notes

- Make sure to handle all relevant media actions for a smooth user experience across all platforms.
- The plugin starts a foreground service on Android to enable background playback and show the media notification.
- Artwork URLs can be remote (http/https) or data URLs.

## Known Issues

- On some Android devices, the media notification might not show immediately after setting metadata. This is usually resolved once playback begins.
- Seeking on iOS might not be as precise as on Android due to limitations in the Web Media Session API implementation on iOS.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Support

If you're having any problem, please [raise an issue](https://github.com/marcellov7/cordova-plugin-media-session/issues) on GitHub and we'll be happy to help.

## Credits
