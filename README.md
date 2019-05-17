基于android-eros-plugin-amap修改
参考高德地图示例https://lbs.amap.com/dev/demo/place-choose#Android
增加了weex-amap和weex-amap-marker组件的fixed属性，来判断marker是否固定，即不会随地图的移动而移动
weex-amap组件新增了camerachange事件，每次拖动地图都会触发，传递地图的中心点，即maker的坐标给js
新增geoAddress逆地址编码查询，传入maker的坐标，得到位置信息，实现地图选址功能。


组件代码：
<weex-amap class="map" :fixed="true" id="map2017" zoom="16" scale="true" geolocation="true" :center="pos" @camerachange="camerachange" gestures="[zoom,scroll]">
      <weex-amap-marker :fixed="true" :position="point.position" :title="point.title" :icon="point.icon"></weex-amap-marker>
</weex-amap>

js调用代码：
var amap = weex.requireModule('amap')
camerachange(e) {
  amap.geoAddress(e.centerPosition, data => {
    console.log('callback结果：' + JSON.stringify(data))
    this.address = data.address
    this.params = data
    this.params.centerPosition = e.centerPosition
  })
}

详细修改代码diff查看
https://github.com/weue/eros-plugin-android-amap/commit/ad6214402dd72eee352f2de9bc4cf0e3c355bd07