ChessBoardLayout
================

![ChessBoardLayout](https://raw.githubusercontent.com/KaiWooram/ChessBoardLayout/master/chessboardlayout.png)

ChessBoardLayout is Android GridView Like Layout which support wrap_content height and Adapter

Android platform gridview is not suitable for fixed height grid layout. To make grid like layout in android, we should use multiple linear layout or make new custom layout which extends gridview and override onmeasure to make it support fixed height. This is not a good solution and make bunch of redundant functions. This is why I decide to make ChessBoardLayout.

ChessBoardLayout is simple gridview like layout. Easy to use and lightweight. Currently not very stable but will be maintained. 

## Install Guide

1. You can just clone the project and use it as android library module.
2. Maven repository will be soon supported.

## Usage

- Fastest way is see the [sample project](https://github.com/KaiWooram/ChessBoardLayout/tree/master/sample).

#### define ChessBoardLayout in your android layout(xml)
```
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:cl="http://schemas.android.com/apk/res-auto">
    
    <com.jungkai.chessboardlayout.ChessBoardLayout
      android:id="@+id/layout"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      cl:colCount="3"
      cl:colSpacing="1px"
      cl:rowSpacing="1px"/>
      
</LinearLayout>
```

#### Attributes
- colCount: grid column count (default : 3)
- colSpacing: grid column spacing (default : 0)
- rowSpacing: grid row spacing (default : 0)

will be updated soon!








