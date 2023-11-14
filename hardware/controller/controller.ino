#include <EEPROM.h>
#include <SoftwareSerial.h>
#include <ArduinoJson.h>
#include <RTClib.h>
#include <LedControl.h>

// Arduino Pins
#define DATA_PIN  10
#define CLK_PIN   11
#define LOAD_PIN  9
#define ROW_COUNT 4

#define ERR       1

// EEPROM addresses
#define LYEAR_ADDR 0
#define LMON_ADDR  LYEAR_ADDR + sizeof(uint16_t)
#define LDAY_ADDR  LMON_ADDR + sizeof(uint8_t)
#define DWF_ADDR   LDAY_ADDR + sizeof(uint8_t)
#define FAC_ADDR   DWF_ADDR + sizeof(uint16_t)
#define RI_ADDR    FAC_ADDR + sizeof(uint16_t)
#define DWA_ADDR   RI_ADDR + sizeof(uint16_t)
#define MODE_ADDR  DWA_ADDR + sizeof(uint16_t)

enum MODE {
  CLEAR,
  TEST,
  DATA
};
enum ROW_PARTITION {
  FULL,
  LOW_HALF,
  HIGH_HALF
};
enum ROW_ALIGNMENT {
  LEFT,
  RIGHT
};

SoftwareSerial bluetooth(7, 8); // RX & TX
RTC_DS1307 rtc;
DynamicJsonDocument doc(512);
LedControl lc = LedControl(DATA_PIN, CLK_PIN, LOAD_PIN, ROW_COUNT);
byte mode;
uint32_t ctime, ltime;
uint16_t cyear, lyear, dwf, fac, ri, dwa;;
uint8_t cmon, cday, chour, cmin, csec, lmon, lday;
unsigned long current_time, rtime;

char* formatDate(uint16_t, uint8_t, uint8_t);
void printRow(byte, ROW_PARTITION, ROW_ALIGNMENT, char*, bool = false);
int  processBluetoothInput(char*, JsonVariant);
void refreshCurrentDate();
void sendBluetoothStatus(char*, char*);
void setMode(MODE);
void storeEEPROM();
void updateDisplayData();
void writeRegister(byte, byte, byte);

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);
  int8_t deviceCount = lc.getDeviceCount();
  if(!rtc.begin()) {
    Serial.println("RTC is not found");
  }
  for(int8_t i = 0; i < ROW_COUNT; i++) {
    lc.shutdown(i, false);
    lc.setIntensity(i, 8);
    lc.clearDisplay(i);
  }
  // Restore EEPROM data
  EEPROM.get(LYEAR_ADDR, lyear);
  EEPROM.get(LMON_ADDR, lmon);
  EEPROM.get(LDAY_ADDR, lday);
  EEPROM.get(DWF_ADDR, dwf);
  EEPROM.get(FAC_ADDR, fac);
  EEPROM.get(RI_ADDR, ri);
  EEPROM.get(DWA_ADDR, dwa);
  EEPROM.get(MODE_ADDR, mode);
  // Get current date from RTC
  refreshCurrentDate();
  rtime = millis();
  setMode(mode);
}

void loop() {
  // Update current date after 10 seconds
  current_time = millis();
  if((current_time - rtime) > 10000 && mode == DATA) {
    refreshCurrentDate();
    printRow(0, FULL, LEFT, formatDate(cyear, cmon, cday), true);
    rtime = current_time;
  }
  // Parse bluetooth input
  if(bluetooth.available()) {
    // Serial.println(bluetooth.readString());
    if(!deserializeJson(doc, bluetooth)) {
      JsonObject obj = doc.as<JsonObject>();
      refreshCurrentDate();
      int stat = 0;
      ctime = ltime = 0;
      for(JsonPair pair : obj) {
        if(processBluetoothInput(pair.key().c_str(), pair.value()) == ERR) {
          stat = ERR;
          break;
        }
      }
      if(stat != ERR) {
        if(ctime == 0) {
          rtc.adjust(DateTime(cyear, cmon, cday, chour, cmin, csec));
        } else {
          DateTime cdt(ctime);
          cyear = cdt.year();
          cmon = cdt.month();
          cday = cdt.day();
          chour = cdt.hour();
          cmin = cdt.minute();
          csec = cdt.second();
          rtc.adjust(cdt);
        }
        if(ltime != 0) {
          DateTime ldt(ltime);
          lyear = ldt.year();
          lmon = ldt.month();
          lday = ldt.day();
        }
        storeEEPROM();
        setMode(mode);
        sendBluetoothStatus("SUCCESS", "Updated successful");
      }
    }
  }
}

char* formatDate(uint16_t year, uint8_t mon, uint8_t day) {
  static char date[9];
  // Clear date by filling with zeros
  for(int8_t i = 0; i < 8; i++) {
    date[i] = '0';
  }
  char day_buff[3];
  char mon_buff[3];
  char year_buff[5];
  if(day >= 1 && day <= 31) {
    itoa(day, day_buff, 10);
  } else {
    day_buff[0] = '\0';
  }
  if(mon >= 1 && mon <= 12) {
    itoa(mon, mon_buff, 10);
  } else {
    mon_buff[0] = '\0';
  }
  if(year >= 1901 && year <= 2038) {
    itoa(year, year_buff, 10);
  } else {
    year_buff[0] = '\0';
  }
  // Make sure day and month have leading zeros if they are single digits
  if(strlen(day_buff) == 1) {
    date[0] = '0';
    date[1] = day_buff[0];
  } else if(strlen(day_buff) == 2) {
    date[0] = day_buff[0];
    date[1] = day_buff[1];
  }
  if(strlen(mon_buff) == 1) {
    date[2] = '0';
    date[3] = mon_buff[0];
  } else if(strlen(mon_buff) == 2) {
    date[2] = mon_buff[0];
    date[3] = mon_buff[1];
  }
  for(int8_t i = 0; i < strlen(year_buff) && i < 4; i++) {
    date[i + 4] = year_buff[i];
  }
  date[8] = '\0'; // Null terminator
  return date;
}

void printRow(byte row, ROW_PARTITION partition, ROW_ALIGNMENT alignment, char* data, bool date = false) {
  size_t len = strlen(data);
  size_t max_len = (partition == FULL) ? 8 : 4;
  len = len > max_len ? max_len : len;
  byte col = 0;
  if(partition == FULL || partition == LOW_HALF) {
    col = (alignment == RIGHT) ? len - 1 : max_len - 1;
  } else if(partition == HIGH_HALF) {
    col = (alignment == RIGHT) ? (len - 1) + 4 : 7;
  }
  for(int8_t i = 0; i < len; i++) {
    if(date && (i == 1 || i == 3)) {
      lc.setChar(row, col--, data[i], true);
    } else {
      lc.setChar(row, col--, data[i], false);
    }
  }
}

int processBluetoothInput(char* key, JsonVariant value) {
  if(strcmp(key, "cyear") == 0) {
    if(!(value.is<uint16_t>() && (cyear = value.as<uint16_t>()) >= 1901 && cyear <= 2038)) {
      sendBluetoothStatus("FAILURE", "Invalid year");
      return 1;
    }
  } else if(strcmp(key, "cmon") == 0) {
    if(!(value.is<uint8_t>() && (cmon = value.as<uint8_t>()) >= 1 && cmon <= 12)) {
      sendBluetoothStatus("FAILURE", "Invalid month");
      return 1;
    }
  } else if(strcmp(key, "cday") == 0) {
    if(!(value.is<uint8_t>() && (cday = value.as<uint8_t>()) >= 1 && cday <= 31)) {
      sendBluetoothStatus("FAILURE", "Invalid day");
      return 1;
    }
  } else if(strcmp(key, "chour") == 0) {
    if(!(value.is<uint8_t>() && (chour = value.as<uint8_t>()) >= 0 && chour <= 23)) {
      sendBluetoothStatus("FAILURE", "Invalid hour");
      return 1;
    }
  } else if(strcmp(key, "cmin") == 0) {
    if(!(value.is<uint8_t>() && (cmin = value.as<uint8_t>()) >= 0 && cmin <= 59)) {
      sendBluetoothStatus("FAILURE", "Invalid minite");
      return 1;
    }
  } else if(strcmp(key, "csec") == 0) {
    if(!(value.is<uint8_t>() && (csec = value.as<uint8_t>()) >= 0 && csec <= 59)) {
      sendBluetoothStatus("FAILURE", "Invalid second");
      return 1;
    }
  } else if(strcmp(key, "lyear") == 0) {
    if(!(value.is<uint16_t>() && (lyear = value.as<uint16_t>()) >= 1901 && lyear <= 2038)) {
      sendBluetoothStatus("FAILURE", "Invalid year");
      return 1;
    }
  } else if(strcmp(key, "lmon") == 0) {
    if(!(value.is<uint8_t>() && (lmon = value.as<uint8_t>()) >= 1 && lmon <= 12)) {
      sendBluetoothStatus("FAILURE", "Invalid month");
      return 1;
    }
  } else if(strcmp(key, "lday") == 0) {
    if(!(value.is<uint8_t>() && (lday = value.as<uint8_t>()) >= 1 && lday <= 31)) {
      sendBluetoothStatus("FAILURE", "Invalid day");
      return 1;
    }
  } else if(strcmp(key, "dwf") == 0) { // YTD presence hours // number
    if(!(value.is<uint16_t>() && (dwf = value.as<uint16_t>()) >= 0 && dwf <= 9999)) {
      sendBluetoothStatus("FAILURE", "Invalid dwf");
      return 1;
    }
  } else if(strcmp(key, "fac") == 0) { // first aid cases // number
    if(!(value.is<uint16_t>() && (fac = value.as<uint16_t>()) >= 0 && fac <= 9999)) {
      sendBluetoothStatus("FAILURE", "Invalid fac");
      return 1;
    }
  } else if(strcmp(key, "ri") == 0) { // recorded incident // number
    if(!(value.is<uint16_t>() && (ri = value.as<uint16_t>()) >= 0 && ri <= 9999)) {
      sendBluetoothStatus("FAILURE", "Invalid ri");
      return 1;
    }
  } else if(strcmp(key, "dwa") == 0) { // days without accident
    if(!(value.is<uint16_t>() && (dwa = value.as<uint16_t>()) >= 0 && dwa <= 9999)) {
      sendBluetoothStatus("FAILURE", "Invalid dwa");
      return 1;
    }
  } else if(strcmp(key, "ctime") == 0) { 
    if(value.is<uint32_t>()) {
      ctime = value.as<uint32_t>();
    } else {
      sendBluetoothStatus("FAILURE", "Invalid ctime");
      return 1;
    }
  } else if(strcmp(key, "ltime") == 0) {
    if(value.is<uint32_t>()) {
      ltime = value.as<uint32_t>();
    } else {
      sendBluetoothStatus("FAILURE", "Invalid ltime");
      return 1;
    }
  } else if(strcmp(key, "cidate") == 0) { // date // 2022-09-03 14:32:40
    if(value.is<const char*>()) {
      DateTime dt(value.as<const char*>());
      cyear = dt.year();
      cmon = dt.month();
      cday = dt.day();
      chour = dt.hour();
      cmin = dt.minute();
      csec = dt.second();
    } else {
      sendBluetoothStatus("FAILURE", "Invalid cidate");
      return 1;
    }
  } else if(strcmp(key, "lidate") == 0) { // date ISO
    if(value.is<const char*>()) {
      DateTime dt(value.as<const char*>());
      lyear = dt.year();
      lmon = dt.month();
      lday = dt.day();
    } else {
      sendBluetoothStatus("FAILURE", "Invalid lidate");
      return 1;
    }
  } else if(strcmp(key, "mode") == 0) {
    if(!(value.is<uint8_t>() && (mode = value.as<uint8_t>()) >= 0 && mode <= 2)) {
      sendBluetoothStatus("FAILURE", "Invalid mode");
      return 1;
    }
  } else if(strcmp(key, "action") == 0) { 
    if(value.is<const char*>() && strcmp(value.as<const char*>(), "get") == 0) { // { "action": "get" }
      JsonObject obj = doc.to<JsonObject>();
      obj["status"] = "SUCCESS";
      obj["cyear"] = cyear;
      obj["cmon"] = cmon;
      obj["cday"] = cday;
      obj["chour"] = chour;
      obj["cmin"] = cmin;
      obj["csec"] = csec;
      obj["lyear"] = lyear;
      obj["lmon"] = lmon;
      obj["lday"] = lday;
      obj["dwf"] = dwf;
      obj["fac"] = fac;
      obj["ri"] = ri;
      obj["dwa"] = dwa;
      obj["mode"] = mode;
      serializeJson(doc, bluetooth);
      // Return 1 to prevent changes
      return 1;
    } else {
      sendBluetoothStatus("FAILURE", "Invalid action");
      return 1;
    }
  } else {
    sendBluetoothStatus("FAILURE", "Invalid key");
    return 1;
  }
  return 0;
}

void refreshCurrentDate() {
  DateTime dt = rtc.now();
  cyear = dt.year();
  cmon = dt.month();
  cday = dt.day();
  chour = dt.hour();
  cmin = dt.minute();
  csec = dt.second();
}

void sendBluetoothStatus(char* stat, char* msg) {
  JsonObject obj = doc.to<JsonObject>();
  obj["status"] = stat;
  obj["message"] = msg;
  serializeJson(doc, bluetooth);
}

void setMode(MODE mode) {
  if(mode == CLEAR) {
    for(int8_t i = 0; i < ROW_COUNT; i++) {
      lc.clearDisplay(i);
    }
  } else if(mode == TEST) {
    for(int8_t i = 0; i < ROW_COUNT; i++) {
      for(int8_t j = 0; j < 8; j++) {
        lc.setChar(i, j, '8', true);
      }
    }
  } else {
    // For data mode
    updateDisplayData();
  }
}

void storeEEPROM() {
  EEPROM.put(LYEAR_ADDR, lyear);
  EEPROM.put(LMON_ADDR, lmon);
  EEPROM.put(LDAY_ADDR, lday);
  EEPROM.put(DWF_ADDR, dwf);
  EEPROM.put(FAC_ADDR, fac);
  EEPROM.put(RI_ADDR, ri);
  EEPROM.put(DWA_ADDR, dwa);
  EEPROM.put(MODE_ADDR, mode);
} 

void updateDisplayData() {
  setMode(CLEAR);
  char dwf_buff[5];
  char ri_buff[5];
  char fac_buff[5];
  char dwa_buff[5];
  if(dwf >= 0 && dwf <= 9999) {
    itoa(dwf, dwf_buff, 10);
  } else {
    dwf_buff[0] = '\0';
  }
  if(ri >= 0 && ri <= 9999) {
    itoa(ri, ri_buff, 10);
  } else {
    ri_buff[0] = '\0';
  }
  if(fac >= 0 && fac <= 9999) {
    itoa(fac, fac_buff, 10);
  } else {
    fac_buff[0] = '\0';
  }
  if(dwa >= 0 && dwa <= 9999) {
    itoa(dwa, dwa_buff, 10);
  } else {
    dwa_buff[0] = '\0';
  }
  printRow(0, FULL, LEFT, formatDate(cyear, cmon, cday), true);
  printRow(1, FULL, LEFT, formatDate(lyear, lmon, lday), true);
  printRow(2, HIGH_HALF, RIGHT, dwf_buff);
  printRow(2, LOW_HALF, RIGHT, ri_buff);
  printRow(3, HIGH_HALF, RIGHT, fac_buff);
  printRow(3, LOW_HALF, RIGHT, dwa_buff);
}
