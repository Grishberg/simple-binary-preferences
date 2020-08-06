
[![](https://jitpack.io/v/Grishberg/simple-binary-preferences.svg)](https://jitpack.io/#Grishberg/simple-binary-preferences)

# Simple Binary Preferences.
Reads and writes values into binary format.

## Performance
Android shared preferences VS  for 10 launches:
Android shared preferences(ms) | simple binary preferences(ms)
--- | ---
3.213542 | 0.755834
3.835781 | 2.886511
4.19849 | 2.704844
6.501615 | 2.619844
3.869219 | 3.087813
3.427708 | 2.950833
4.537761 | 3.129376
6.586198 | 2.668959
7.719167  | 2.840365
4.903334  | 3.027344

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