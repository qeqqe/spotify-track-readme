Minimal service that converts your current spotify track into an embeddable SVG.

# Current playing
![](./assets/active.svg)

# Recently played
![](./assets/recent.svg)

# Installation
1. Rename the `example.application.properties` to `application.properties`
   ```bash
   mv ./src/main/resources/example.application.properties appliaction.properties
   ```
2. Go to `https://open.spotify.com/` and copy your `sp_dc` and paste it in the `application.properties` as `spotify.sp_dc=your-spotify-sp_dc-cookie`
3. build the application
    ```bash
      ./mvnw clean install
    ```
4. Use the `target/spotify-current-readme-0.0.1-SNAPSHOT.jar` and deploy your application.

# sample
check out my profile! https://github.com/qeqqe