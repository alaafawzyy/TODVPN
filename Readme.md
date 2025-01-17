# BasedVPN Android

## Project setup

Before building and running the project, some initial setup is needed.

BuildConfig file needs access to `BASED_VPN_MAPS_KEY` system environment variable. This variable can be set through terminal using commands in next section.

<details><summary>For bash</summary>
<p>

### For bash:

1. Open `~/.bash_profile` file with preferred editor
   ```
   nano ~/.bash_profile
   vim ~/.bash_profile
   vi ~/.bash_profile
   ```
2. Add `export BASED_VPN_MAPS_KEY=XXXX`
3. Save file
4. Execute `source ~/.bash_profile`to load environmental variables into current session
5. Execute `printenv` to check if variable is set

For more detailed explanation check out [Set evrionmental variable in BASH](https://youngstone89.medium.com/setting-up-environment-variables-in-mac-os-28e5941c771c).


</p>
</details>

<details><summary>For zsh</summary>
<p>

### For zsh:

1. Open `~/.zshrc` file with preferred editor
```
   nano ~/.zshrc
   vim ~/.zshrc
   vi ~/.zshrc
   ```
2. Add `export BASED_VPN_MAPS_KEY=XXXX`
3. Save file
4. Execute `source ~/.zshrc`to load environmental variables into current session
5. Execute `printenv` to check if variable is set

For more detailed explanation check out [Set evrionmental variable in ZSH](https://linuxhint.com/set-environment-variable-zsh/).

</p>
</details>

## Publishing

### Maven Local
./gradlew :based_vpn:publishToMavenLocal

### Maven Global
./gradlew :based_vpn:publish
