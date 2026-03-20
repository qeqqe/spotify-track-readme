Minimal service that converts your current last.fm playing scrobbled track into an embeddable SVG.

# Current playing

![](./assets/active.svg)

# Recently played

![](./assets/recent.svg)

# Installation

1. Rename the `example.application.properties` to `application.properties`

   ```bash
   mv ./src/main/resources/example.application.properties application.properties
   ```

2. **Add credentials to `./application.properties`**

```sh

last.fm.api.key=your-last-fm-api-key
last.fm.username=your-username
```

3. build the application
   ```bash
     ./mvnw clean install
   ```
4. Use the `target/current-track-readme-0.0.1-SNAPSHOT.jar` and deploy your application.

# sample

check out my profile! https://github.com/qeqqe

