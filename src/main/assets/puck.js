var a = [26, 255, 76, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 100, 100, 100, 10];
var count_pos = 7;
var bat_pos = 6;
var curr_count = 0;
var update_advertising_interval = 500;
var advertising_broadcast_interval = 150;
var zero = Puck.mag();
var threshold = 2500;
var haveEvent = true;
var makeZero = false;
var magSetting = 1.25;

function zero_array() {
    a[7] = 0;
    a[8] = 0;
    a[9] = 0;
    a[10] = 0;
    a[11] = 0;
    a[12] = 0;
    a[13] = 0;
    a[14] = 0;
    a[15] = 0;
    a[16] = 0;
    a[17] = 0;
    a[18] = 0;
    a[19] = 0;
    a[20] = 0;
    a[21] = 0;
}

function updateCount() {
    curr_count++;
    if (curr_count > 255) {
        curr_count = 0;
    }
    a[count_pos] = curr_count;
}

function doorOpenCheck(p) {
    p.x -= zero.x;
    p.y -= zero.y;
    p.z -= zero.z;
    var triangular_dist = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
    var isEvent = triangular_dist > threshold;
    if (isEvent !== haveEvent) {
        Bluetooth.println('$'+!isEvent+'$'+Puck.getBatteryPercentage()+'$');
        haveEvent = isEvent;
        updateCount();
        digitalPulse(isEvent ? LED2 : LED1, 1, 1000);
    }
}
clearWatch();
setWatch(function() {
    curr_count = 0;
    zero = Puck.mag();
    haveEvent = true;
    print('Refreshing');
    digitalPulse(LED3, 1, 1000);
}, BTN, {
    edge: 'rising',
    debounce: 50,
    repeat: true
});

Puck.on('mag', doorOpenCheck);
function onInit() {
  NRF.setAdvertising({},{name:"VXC DS"});
  Puck.magOn(magSetting);
}
