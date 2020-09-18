
[![](https://jitpack.io/v/Grishberg/simple-binary-preferences.svg)](https://jitpack.io/#Grishberg/simple-binary-preferences)

# Simple Binary Preferences.
Reads and writes values into binary format.

## Usage
```
 SharedPreferences preferences = new BinaryPreferences(context, preferencesName);
```

## Performance
Android shared preferences VS  for 10 launches:
Android shared preferences(ms) | simple binary preferences(ms)
--- | ---
3.111354 | 1.784636
9.030053 | 1.913177
4.566719 | 1.884219
3.478386 | 1.853803
3.491459 | 1.808386
6.951407 | 1.673803
3.399479 | 1.708229
4.412031 | 1.824063
3.609219 | 2.480208
4.235574 | 2.137083

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
