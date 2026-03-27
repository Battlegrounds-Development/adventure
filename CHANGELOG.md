# Changelog

All notable changes to this project will be documented in this file. See [commit-and-tag-version](https://github.com/absolute-version/commit-and-tag-version) for commit guidelines.

## [1.1.1](https://github.com/Battlegrounds-Development/adventure/compare/v1.1.0...v1.1.1) (2026-03-27)

## [1.1.0](https://github.com/Battlegrounds-Development/adventure/compare/v1.0.0...v1.1.0) (2026-02-20)


### Features

* added /bgs adventure to plugin ([f8be78e](https://github.com/Battlegrounds-Development/adventure/commit/f8be78eafd398a487db5258684d0c0c2c0aba8de))
* added PDCManager so players are penalized accordingly on login, and PDC matches up with world. Also fixed bug with boss bars not disappearing. ([8c4d7cc](https://github.com/Battlegrounds-Development/adventure/commit/8c4d7ccc306c0a93ba3ad75426b50d774ec2ed0f))


### Bug Fixes

* stopped task service from registering under name conflicts ([3dd5469](https://github.com/Battlegrounds-Development/adventure/commit/3dd5469c1afb8201b469099b188468b4fd2bb9a7))

# 1.0.0 (2026-01-15)


### Bug Fixes

* **blizzard:** Fixed a syntax error. The correct enum from PotionEffect is now mentioned. ([363d853](https://github.com/Remag501/AdventureBGS/commit/363d853494805d6e0e76214346de9dd07bea7483))
* **weather:** Uses paper async place to not crash server when changing beacon colors. ([7e84c1c](https://github.com/Remag501/AdventureBGS/commit/7e84c1c96d26fd3253ebab4da6a7c49ab5b392e3))


### Features

* added new placeholders %adventurebgs_next_world_id% and %adventurebgs_current_world_id%. ([e07571f](https://github.com/Remag501/AdventureBGS/commit/e07571f9c2842d51eb0529e4d528c6b3856e14dc))
* added placeholder api hook support for commands. ([3018802](https://github.com/Remag501/AdventureBGS/commit/3018802ebace80a0b24e771e0a4de675d7eb390e))
* broadcast title with warning, and puts timer boss bar near map closure. Applies wither to correct map. Kills any offline player that disconnected in map during closure. ([a35df26](https://github.com/Remag501/AdventureBGS/commit/a35df2600e2e06b27e90e8810edf16665e1fd4a7))
* changed rejoin in map penalty to a wither effect instead of instant death ([80fda9d](https://github.com/Remag501/AdventureBGS/commit/80fda9dd33a8ede61635659e861c8ebe2bb5699c))
* **weather:** Add blizzard effect class to give players slowness and spawn snowflake particle. Called on function tick. ([d32f2ef](https://github.com/Remag501/AdventureBGS/commit/d32f2efa2392eedd2572f728461578e74cbadc94))
* **weather:** Blizzard has been updated to have its intended features. Player set freeze over time and heat source to remove it. ([fc83702](https://github.com/Remag501/AdventureBGS/commit/fc83702ae352bce9d94056a45a993fc67130d73f))
* **weather:** Introduce the 'weather_conditions' list structure in config.yml to support unique properties for Blizzard and future weather. Created empty manager and model file. ([5251a91](https://github.com/Remag501/AdventureBGS/commit/5251a914c769c9008d291e2687e144496823bb5a))
* **weather:** Manager loads weather model from config and assigns schedules weather based on frequency and intervals. ([804fd7d](https://github.com/Remag501/AdventureBGS/commit/804fd7dfea03aca098a6dc0856b39ef604a052aa))
* **weather:** Model stores data for weather conditions. ([fafc198](https://github.com/Remag501/AdventureBGS/commit/fafc1981b4164eebbad094b72567ecbb1697d484))
