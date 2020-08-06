
[![](https://jitpack.io/v/Grishberg/simple-binary-preferences.svg)](https://jitpack.io/#Grishberg/simple-binary-preferences)

# Simple Binary Preferences.
Reads and writes values into binary format.

## Performance
Android shared preferences VS simple binary preferences for 10 launches:
android : 3.213542 ms VS binary prefs: 0.755834 ms
android : 3.835781 ms VS binary prefs: 2.886511 ms
android : 4.19849 ms VS binary prefs: 2.704844 ms
android : 6.501615 ms VS binary prefs: 2.619844 ms
android : 3.869219 ms VS binary prefs: 3.087813 ms
android : 3.427708 ms VS binary prefs: 2.950833 ms
android : 4.537761 ms VS binary prefs: 3.129376 ms
android : 6.586198 ms VS binary prefs: 2.668959 ms
android : 7.719167 ms VS binary prefs: 2.840365 ms
android : 4.903334 ms VS binary prefs: 3.027344 ms
android : 4.220209 ms VS binary prefs: 3.094531 ms

measured on Nexus 5X

## Dependencies

1) Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
	    ...
		maven { url 'https://jitpack.io' }
	}
}
```

2) Step 2. Add the dependency
```
dependencies {
    implementation "com.github.Grishberg:simple-binary-preferences:$version"
}
```