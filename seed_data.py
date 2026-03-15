#!/usr/bin/env python3
"""AuctionDeck Seed v2 - register -> approve -> add-to-auction -> sell"""
import json, sys, urllib.request, urllib.error
BASE = "http://localhost:8080/api"
def _call(method, path, body=None, token=None, ct="application/json"):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data,
                                  headers={"Content-Type": ct}, method=method)
    if token: req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as r: return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"  ✗ {method} {path} -> {e.code}: {e.read().decode()[:200]}"); return None
def post(path, body, token=None): return _call("POST", path, body, token)
def patch(path, body, token):     return _call("PATCH", path, body, token)
def get(path, token):
    req = urllib.request.Request(f"{BASE}{path}")
    req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as r: return json.loads(r.read())
    except: return None
def delete(path, token):
    req = urllib.request.Request(f"{BASE}{path}", method="DELETE")
    req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req): return True
    except urllib.error.HTTPError as e:
        print(f"  ✗ DELETE {path} -> {e.code}"); return False
def mp_post(path, fields, token=None):
    boundary = "----ADBoundary"
    parts = [f"--{boundary}\r\nContent-Disposition: form-data; name=\"{k}\"\r\n\r\n{v}\r\n" for k,v in fields.items()]
    parts.append(f"--{boundary}--\r\n")
    body = "".join(parts).encode()
    req = urllib.request.Request(f"{BASE}{path}", data=body,
          headers={"Content-Type": f"multipart/form-data; boundary={boundary}"}, method="POST")
    if token: req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as r: return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"  ✗ POST(mp) {path} -> {e.code}: {e.read().decode()[:200]}"); return None
def ok(lbl, o):
    if o:
        n=(o.get("name") or (o.get("firstName","")+' '+o.get("lastName","")).strip() or o.get("status",""))
        print(f"  + {lbl} -> id={o.get('id','?')} {n}")
USERS=[
    {"name":"Ravi Kumar","email":"ravi@auctiondeck.com","password":"Admin@123","phoneCountryCode":"+91","phoneNumber":"9876543210","organisation":"Mumbai Sports Club","sport":"Cricket","numberOfTeams":8},
    {"name":"Priya Sharma","email":"priya@auctiondeck.com","password":"Admin@123","phoneCountryCode":"+91","phoneNumber":"9123456780","organisation":"Delhi Cricket Academy","sport":"Cricket","numberOfTeams":6},
    {"name":"Amit Patel","email":"amit@auctiondeck.com","password":"Admin@123","phoneCountryCode":"+91","phoneNumber":"9988776655","organisation":"Gujarat Cricket Board","sport":"Cricket","numberOfTeams":8},
    {"name":"Sneha Singh","email":"sneha@auctiondeck.com","password":"Admin@123","phoneCountryCode":"+91","phoneNumber":"8877665544","organisation":"North India Sports","sport":"Cricket","numberOfTeams":6},
]
TOURNAMENTS=[
    (0,{"name":"IPL Mini 2026","date":"2026-04-10","sport":"Cricket","totalTeams":"8","totalPlayers":"120","purseAmount":"1000000","playersPerTeam":"15","basePrice":"20000","status":"UPCOMING"}),
    (0,{"name":"Mumbai T20 Blast","date":"2026-05-15","sport":"Cricket","totalTeams":"6","totalPlayers":"90","purseAmount":"750000","playersPerTeam":"15","basePrice":"15000","status":"ONGOING"}),
    (1,{"name":"Women's Premier League 2026","date":"2026-03-25","sport":"Cricket","totalTeams":"4","totalPlayers":"60","purseAmount":"500000","playersPerTeam":"15","basePrice":"10000","status":"UPCOMING"}),
    (1,{"name":"Corporate Cricket Cup","date":"2026-06-01","sport":"Cricket","totalTeams":"8","totalPlayers":"120","purseAmount":"800000","playersPerTeam":"15","basePrice":"20000","status":"UPCOMING"}),
    (1,{"name":"Weekend Warriors T20","date":"2025-12-10","sport":"Cricket","totalTeams":"6","totalPlayers":"90","purseAmount":"600000","playersPerTeam":"15","basePrice":"15000","status":"COMPLETED"}),
    (2,{"name":"Gujarat Premier League","date":"2026-04-20","sport":"Cricket","totalTeams":"8","totalPlayers":"120","purseAmount":"1200000","playersPerTeam":"15","basePrice":"25000","status":"ONGOING"}),
    (2,{"name":"Surat Super League","date":"2026-07-05","sport":"Cricket","totalTeams":"6","totalPlayers":"90","purseAmount":"900000","playersPerTeam":"15","basePrice":"20000","status":"UPCOMING"}),
    (2,{"name":"Navratri Cup 2026","date":"2026-10-02","sport":"Cricket","totalTeams":"8","totalPlayers":"120","purseAmount":"1000000","playersPerTeam":"15","basePrice":"20000","status":"UPCOMING"}),
    (2,{"name":"Diwali T20 Cup","date":"2025-11-01","sport":"Cricket","totalTeams":"4","totalPlayers":"60","purseAmount":"500000","playersPerTeam":"15","basePrice":"10000","status":"COMPLETED"}),
    (3,{"name":"Delhi Smashers League","date":"2026-05-01","sport":"Cricket","totalTeams":"6","totalPlayers":"90","purseAmount":"800000","playersPerTeam":"15","basePrice":"20000","status":"UPCOMING"}),
    (3,{"name":"North India T20 Championship","date":"2026-08-15","sport":"Cricket","totalTeams":"8","totalPlayers":"120","purseAmount":"1500000","playersPerTeam":"15","basePrice":"30000","status":"ONGOING"}),
]
TEAMS={
0:[{"teamNumber":"T001","name":"Mumbai Titans","ownerName":"Rohit Mehta","mobileNumber":"9001001001"},{"teamNumber":"T002","name":"Delhi Dynamos","ownerName":"Vikas Kapoor","mobileNumber":"9002002002"},{"teamNumber":"T003","name":"Chennai Kings","ownerName":"Suresh Iyer","mobileNumber":"9003003003"},{"teamNumber":"T004","name":"Kolkata Knights","ownerName":"Abhijit Roy","mobileNumber":"9004004004"}],
1:[{"teamNumber":"T001","name":"Bandra Blasters","ownerName":"Nikhil Joshi","mobileNumber":"9005005005"},{"teamNumber":"T002","name":"Andheri Avengers","ownerName":"Pradeep Nair","mobileNumber":"9006006006"},{"teamNumber":"T003","name":"Thane Thunder","ownerName":"Kiran Patil","mobileNumber":"9007007007"}],
2:[{"teamNumber":"T001","name":"Pink Panthers","ownerName":"Anjali Rao","mobileNumber":"9010010010"},{"teamNumber":"T002","name":"Scarlet Strikers","ownerName":"Deepa Menon","mobileNumber":"9011011011"},{"teamNumber":"T003","name":"Purple Eagles","ownerName":"Reena Gupta","mobileNumber":"9012012012"},{"teamNumber":"T004","name":"Golden Falcons","ownerName":"Sunita Verma","mobileNumber":"9013013013"}],
3:[{"teamNumber":"T001","name":"TechCorp Titans","ownerName":"Arun Sharma","mobileNumber":"9020020020"},{"teamNumber":"T002","name":"FinEdge Falcons","ownerName":"Manish Jain","mobileNumber":"9021021021"},{"teamNumber":"T003","name":"StartUp Stars","ownerName":"Rahul Bajaj","mobileNumber":"9022022022"},{"teamNumber":"T004","name":"BankIT Bulls","ownerName":"Sanjay Mishra","mobileNumber":"9023023023"}],
4:[{"teamNumber":"T001","name":"Saturday Slashers","ownerName":"Vikram Ahuja","mobileNumber":"9030030030"},{"teamNumber":"T002","name":"Sunday Sixes","ownerName":"Harish Reddy","mobileNumber":"9031031031"},{"teamNumber":"T003","name":"Holiday Heroes","ownerName":"Kapil Yadav","mobileNumber":"9032032032"}],
5:[{"teamNumber":"T001","name":"Ahmedabad Arrows","ownerName":"Jigar Desai","mobileNumber":"9040040040"},{"teamNumber":"T002","name":"Surat Sixers","ownerName":"Dhaval Shah","mobileNumber":"9041041041"},{"teamNumber":"T003","name":"Vadodara Vipers","ownerName":"Ketan Patel","mobileNumber":"9042042042"},{"teamNumber":"T004","name":"Rajkot Royals","ownerName":"Bhavesh Modi","mobileNumber":"9043043043"}],
6:[{"teamNumber":"T001","name":"Diamond City XI","ownerName":"Hitesh Vora","mobileNumber":"9050050050"},{"teamNumber":"T002","name":"Surat Stallions","ownerName":"Paresh Gajjar","mobileNumber":"9051051051"},{"teamNumber":"T003","name":"Textile Tigers","ownerName":"Nayan Trivedi","mobileNumber":"9052052052"}],
7:[{"teamNumber":"T001","name":"Garba Warriors","ownerName":"Rajesh Raval","mobileNumber":"9060060060"},{"teamNumber":"T002","name":"Dandiya Kings","ownerName":"Sunil Thakor","mobileNumber":"9061061061"},{"teamNumber":"T003","name":"Festival Flames","ownerName":"Devang Pandya","mobileNumber":"9062062062"},{"teamNumber":"T004","name":"Heritage Hawks","ownerName":"Parth Bhatt","mobileNumber":"9063063063"}],
8:[{"teamNumber":"T001","name":"Diwali Dynamos","ownerName":"Mehul Joshi","mobileNumber":"9070070070"},{"teamNumber":"T002","name":"Firecracker XI","ownerName":"Rupesh Parikh","mobileNumber":"9071071071"},{"teamNumber":"T003","name":"Gold Rush Giants","ownerName":"Tushar Chauhan","mobileNumber":"9072072072"}],
9:[{"teamNumber":"T001","name":"Capital Crushers","ownerName":"Arjun Malhotra","mobileNumber":"9080080080"},{"teamNumber":"T002","name":"Yamuna Yodhas","ownerName":"Sumit Tyagi","mobileNumber":"9081081081"},{"teamNumber":"T003","name":"Red Fort Raiders","ownerName":"Gaurav Saxena","mobileNumber":"9082082082"},{"teamNumber":"T004","name":"Metro Mavericks","ownerName":"Vivek Chauhan","mobileNumber":"9083083083"}],
10:[{"teamNumber":"T001","name":"Punjab Lions","ownerName":"Harpreet Singh","mobileNumber":"9090090090"},{"teamNumber":"T002","name":"Haryana Hawks","ownerName":"Sandeep Hooda","mobileNumber":"9091091091"},{"teamNumber":"T003","name":"UP Unicorns","ownerName":"Akhilesh Yadav Jr","mobileNumber":"9092092092"},{"teamNumber":"T004","name":"Rajasthan Royals XI","ownerName":"Digvijay Rathore","mobileNumber":"9093093093"}],
}
PLAYERS={
0:[
{"reg":{"firstName":"Arjun","lastName":"Sharma","dob":"1999-03-10","role":"Batsman"},"auc":{"age":26,"city":"Mumbai","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Medium","basePrice":50000}},
{"reg":{"firstName":"Virat","lastName":"Kohli","dob":"1988-11-05","role":"Batsman"},"auc":{"age":37,"city":"Delhi","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Medium","basePrice":100000}},
{"reg":{"firstName":"Rohit","lastName":"Singh","dob":"1996-08-22","role":"Batsman"},"auc":{"age":29,"city":"Mumbai","battingStyle":"Right-Hand Bat","bowlingStyle":"Off Break","basePrice":75000}},
{"reg":{"firstName":"Jasprit","lastName":"Kumar","dob":"1993-12-06","role":"Bowler"},"auc":{"age":32,"city":"Ahmedabad","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":80000}},
{"reg":{"firstName":"Hardik","lastName":"Pandya","dob":"1993-10-11","role":"All-Rounder"},"auc":{"age":32,"city":"Vadodara","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast Medium","basePrice":90000}},
{"reg":{"firstName":"Rishabh","lastName":"Pant","dob":"1997-10-04","role":"Wicket Keeper"},"auc":{"age":28,"city":"Roorkee","battingStyle":"Left-Hand Bat","bowlingStyle":"None","basePrice":70000}},
{"reg":{"firstName":"KL","lastName":"Rahul","dob":"1992-04-18","role":"Wicket Keeper"},"auc":{"age":34,"city":"Bangalore","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":85000}},
{"reg":{"firstName":"Ravindra","lastName":"Jadeja","dob":"1988-12-06","role":"All-Rounder"},"auc":{"age":37,"city":"Jamnagar","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Orthodox","basePrice":95000}},
],
1:[
{"reg":{"firstName":"Sachin","lastName":"Tendulkar Jr","dob":"2003-07-15","role":"Batsman"},"auc":{"age":22,"city":"Mumbai","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":30000}},
{"reg":{"firstName":"Suryakumar","lastName":"Yadav","dob":"1990-09-22","role":"Batsman"},"auc":{"age":35,"city":"Mumbai","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Medium","basePrice":55000}},
{"reg":{"firstName":"Ishan","lastName":"Kishan","dob":"1998-07-18","role":"Wicket Keeper"},"auc":{"age":27,"city":"Patna","battingStyle":"Left-Hand Bat","bowlingStyle":"None","basePrice":45000}},
{"reg":{"firstName":"Tilak","lastName":"Varma","dob":"2002-11-08","role":"All-Rounder"},"auc":{"age":23,"city":"Hyderabad","battingStyle":"Left-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":35000}},
{"reg":{"firstName":"Shubman","lastName":"Gill","dob":"1999-09-08","role":"Batsman"},"auc":{"age":26,"city":"Fazilka","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":60000}},
{"reg":{"firstName":"Mohammed","lastName":"Siraj","dob":"1994-03-13","role":"Bowler"},"auc":{"age":31,"city":"Hyderabad","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":50000}},
],
2:[
{"reg":{"firstName":"Smriti","lastName":"Mandhana","dob":"1996-07-18","role":"Batsman"},"auc":{"age":29,"city":"Mumbai","battingStyle":"Left-Hand Bat","bowlingStyle":"Right-Arm Medium","basePrice":40000}},
{"reg":{"firstName":"Harmanpreet","lastName":"Kaur","dob":"1989-03-08","role":"All-Rounder"},"auc":{"age":36,"city":"Moga","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":45000}},
{"reg":{"firstName":"Deepti","lastName":"Sharma","dob":"1997-08-24","role":"All-Rounder"},"auc":{"age":28,"city":"Agra","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":35000}},
{"reg":{"firstName":"Richa","lastName":"Ghosh","dob":"2004-06-06","role":"Wicket Keeper"},"auc":{"age":21,"city":"Siliguri","battingStyle":"Right-Hand Bat","bowlingStyle":"None","basePrice":30000}},
{"reg":{"firstName":"Shafali","lastName":"Verma","dob":"2004-01-07","role":"Batsman"},"auc":{"age":22,"city":"Rohtak","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":35000}},
{"reg":{"firstName":"Renuka","lastName":"Singh","dob":"1997-12-08","role":"Bowler"},"auc":{"age":28,"city":"Shimla","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Medium Fast","basePrice":30000}},
],
3:[
{"reg":{"firstName":"Aditya","lastName":"Birla","dob":"1997-05-14","role":"Batsman"},"auc":{"age":28,"city":"Mumbai","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Medium","basePrice":20000}},
{"reg":{"firstName":"Siddharth","lastName":"Kumar","dob":"2000-02-20","role":"Bowler"},"auc":{"age":25,"city":"Pune","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Fast","basePrice":25000}},
{"reg":{"firstName":"Rajan","lastName":"Mehta","dob":"1993-06-30","role":"All-Rounder"},"auc":{"age":32,"city":"Delhi","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":22000}},
{"reg":{"firstName":"Tejas","lastName":"Patel","dob":"1998-09-17","role":"Wicket Keeper"},"auc":{"age":27,"city":"Surat","battingStyle":"Right-Hand Bat","bowlingStyle":"None","basePrice":20000}},
{"reg":{"firstName":"Naveen","lastName":"Reddy","dob":"2001-03-11","role":"Bowler"},"auc":{"age":24,"city":"Hyderabad","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Medium","basePrice":23000}},
],
4:[
{"reg":{"firstName":"Karthik","lastName":"Subramanian","dob":"1995-07-22","role":"Batsman"},"auc":{"age":30,"city":"Chennai","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":15000}},
{"reg":{"firstName":"Aniket","lastName":"Thakur","dob":"1998-04-05","role":"Bowler"},"auc":{"age":27,"city":"Thane","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Fast Medium","basePrice":15000}},
{"reg":{"firstName":"Rahul","lastName":"Dravid Jr","dob":"2002-10-12","role":"Batsman"},"auc":{"age":23,"city":"Bangalore","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Medium","basePrice":15000}},
],
5:[
{"reg":{"firstName":"Axar","lastName":"Patel","dob":"1994-01-20","role":"All-Rounder"},"auc":{"age":32,"city":"Anand","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Orthodox","basePrice":60000}},
{"reg":{"firstName":"Mayank","lastName":"Agarwal","dob":"1991-02-16","role":"Batsman"},"auc":{"age":34,"city":"Bangalore","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":50000}},
{"reg":{"firstName":"Prithvi","lastName":"Shaw","dob":"1999-11-09","role":"Batsman"},"auc":{"age":26,"city":"Mumbai","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":45000}},
{"reg":{"firstName":"Yuzvendra","lastName":"Chahal","dob":"1990-07-23","role":"Bowler"},"auc":{"age":35,"city":"Jind","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Leg Break","basePrice":55000}},
{"reg":{"firstName":"Shardul","lastName":"Thakur","dob":"1991-10-16","role":"All-Rounder"},"auc":{"age":34,"city":"Palghar","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast Medium","basePrice":60000}},
{"reg":{"firstName":"Krunal","lastName":"Pandya","dob":"1991-03-24","role":"All-Rounder"},"auc":{"age":34,"city":"Vadodara","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Off Break","basePrice":55000}},
{"reg":{"firstName":"Umesh","lastName":"Yadav","dob":"1987-10-25","role":"Bowler"},"auc":{"age":38,"city":"Nagpur","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":45000}},
{"reg":{"firstName":"Shreyas","lastName":"Iyer","dob":"1994-12-06","role":"Batsman"},"auc":{"age":31,"city":"Mumbai","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":75000}},
],
6:[
{"reg":{"firstName":"Divyang","lastName":"Hingrajia","dob":"1998-05-20","role":"Bowler"},"auc":{"age":27,"city":"Surat","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":20000}},
{"reg":{"firstName":"Jatin","lastName":"Saxena","dob":"2000-08-11","role":"Bowler"},"auc":{"age":25,"city":"Surat","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Spin","basePrice":22000}},
{"reg":{"firstName":"Akash","lastName":"Soni","dob":"2002-03-14","role":"Batsman"},"auc":{"age":23,"city":"Surat","battingStyle":"Right-Hand Bat","bowlingStyle":"None","basePrice":20000}},
{"reg":{"firstName":"Pratham","lastName":"Gandhi","dob":"2003-11-30","role":"All-Rounder"},"auc":{"age":22,"city":"Bharuch","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Medium","basePrice":22000}},
],
7:[
{"reg":{"firstName":"Hardik","lastName":"Patel","dob":"2001-06-18","role":"Bowler"},"auc":{"age":24,"city":"Ahmedabad","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":20000}},
{"reg":{"firstName":"Mitesh","lastName":"Shah","dob":"1997-04-22","role":"Batsman"},"auc":{"age":28,"city":"Ahmedabad","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":25000}},
{"reg":{"firstName":"Chirag","lastName":"Raval","dob":"1999-09-05","role":"All-Rounder"},"auc":{"age":26,"city":"Gandhinagar","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Medium","basePrice":22000}},
{"reg":{"firstName":"Bhavin","lastName":"Solanki","dob":"2000-12-15","role":"Wicket Keeper"},"auc":{"age":25,"city":"Vadodara","battingStyle":"Right-Hand Bat","bowlingStyle":"None","basePrice":20000}},
{"reg":{"firstName":"Kiran","lastName":"Doshi","dob":"1996-02-28","role":"Bowler"},"auc":{"age":30,"city":"Rajkot","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Spin","basePrice":21000}},
],
8:[
{"reg":{"firstName":"Dhruv","lastName":"Jurel","dob":"2002-09-16","role":"Wicket Keeper"},"auc":{"age":23,"city":"Agra","battingStyle":"Right-Hand Bat","bowlingStyle":"None","basePrice":10000}},
{"reg":{"firstName":"Nitish","lastName":"Rana","dob":"1996-12-27","role":"All-Rounder"},"auc":{"age":29,"city":"Delhi","battingStyle":"Left-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":12000}},
{"reg":{"firstName":"Saurabh","lastName":"Tiwary","dob":"1991-06-07","role":"Batsman"},"auc":{"age":34,"city":"Jharkhand","battingStyle":"Left-Hand Bat","bowlingStyle":"None","basePrice":10000}},
],
9:[
{"reg":{"firstName":"Shikhar","lastName":"Dhawan","dob":"1985-12-05","role":"Batsman"},"auc":{"age":40,"city":"Delhi","battingStyle":"Left-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":50000}},
{"reg":{"firstName":"Virender","lastName":"Sehwag Jr","dob":"2000-10-20","role":"Batsman"},"auc":{"age":25,"city":"Najafgarh","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":40000}},
{"reg":{"firstName":"Amit","lastName":"Mishra","dob":"1982-11-24","role":"Bowler"},"auc":{"age":43,"city":"Aligarh","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Leg Break","basePrice":35000}},
{"reg":{"firstName":"Ishant","lastName":"Sharma","dob":"1988-09-02","role":"Bowler"},"auc":{"age":37,"city":"Delhi","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":45000}},
{"reg":{"firstName":"Navdeep","lastName":"Saini","dob":"1996-11-23","role":"Bowler"},"auc":{"age":29,"city":"Karnal","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":40000}},
{"reg":{"firstName":"Lalit","lastName":"Yadav","dob":"1999-11-09","role":"All-Rounder"},"auc":{"age":26,"city":"Delhi","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":35000}},
],
10:[
{"reg":{"firstName":"Shubman","lastName":"Arora","dob":"2001-07-04","role":"Bowler"},"auc":{"age":24,"city":"Amritsar","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast","basePrice":30000}},
{"reg":{"firstName":"Gurkeerat","lastName":"Singh","dob":"1992-06-10","role":"Batsman"},"auc":{"age":33,"city":"Ludhiana","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":35000}},
{"reg":{"firstName":"Mandeep","lastName":"Singh","dob":"1993-10-18","role":"Batsman"},"auc":{"age":32,"city":"Fazilka","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Off Break","basePrice":40000}},
{"reg":{"firstName":"Arshdeep","lastName":"Singh Jr","dob":"2000-06-05","role":"Bowler"},"auc":{"age":25,"city":"Fatehgarh","battingStyle":"Left-Hand Bat","bowlingStyle":"Left-Arm Fast Medium","basePrice":45000}},
{"reg":{"firstName":"Rahul","lastName":"Tewatia","dob":"1993-08-12","role":"All-Rounder"},"auc":{"age":32,"city":"Farrukhabad","battingStyle":"Left-Hand Bat","bowlingStyle":"Right-Arm Leg Break","basePrice":40000}},
{"reg":{"firstName":"Rishi","lastName":"Dhawan","dob":"1993-02-02","role":"All-Rounder"},"auc":{"age":33,"city":"Hamirpur","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Fast Medium","basePrice":35000}},
{"reg":{"firstName":"Piyush","lastName":"Chawla","dob":"1988-12-24","role":"Bowler"},"auc":{"age":37,"city":"Aligarh","battingStyle":"Right-Hand Bat","bowlingStyle":"Right-Arm Leg Break","basePrice":30000}},
],
}
DR=[{"fromAmount":0,"toAmount":50000,"incrementBy":5000},{"fromAmount":50000,"toAmount":100000,"incrementBy":10000},{"fromAmount":100000,"toAmount":500000,"incrementBy":25000},{"fromAmount":500000,"toAmount":0,"incrementBy":50000}]
SR=[{"fromAmount":0,"toAmount":20000,"incrementBy":2000},{"fromAmount":20000,"toAmount":50000,"incrementBy":5000},{"fromAmount":50000,"toAmount":100000,"incrementBy":10000},{"fromAmount":100000,"toAmount":0,"incrementBy":20000}]
def main():
    print("\n=== AuctionDeck Seed v2: register -> approve -> add-to-auction ===\n")
    tokens={}
    for u in USERS:
        post("/auth/register",u)
        r=post("/auth/login",{"email":u["email"],"password":u["password"]})
        if r and "token" in r: tokens[u["email"]]=r["token"]; print(f"  + Logged in: {u['name']}")
        else: print(f"  ! Login failed {u['email']}"); sys.exit(1)
    print("\n-- Cleanup existing tournaments --")
    for u in USERS:
        for t in get("/tournaments",tokens[u["email"]]) or []:
            if delete(f"/tournaments/{t['id']}",tokens[u["email"]]): print(f"  - Deleted T{t['id']} {t['name']}")
    print("\n-- Create tournaments --")
    t_ids=[]; t_tok=[]
    for ui,f in TOURNAMENTS:
        tok=tokens[USERS[ui]["email"]]
        r=mp_post("/tournaments",f,tok)
        t_ids.append(r["id"] if r else None); t_tok.append(tok)
        if r: ok(f"Tournament ({USERS[ui]['name']})",r)
    print("\n-- Create teams --")
    team_ids={}
    for fi,tid in enumerate(t_ids):
        team_ids[fi]=[]
        if not tid: continue
        for t in TEAMS.get(fi,[]):
            r=mp_post(f"/tournaments/{tid}/teams",t,t_tok[fi])
            if r: ok(f"Team T{tid}",r); team_ids[fi].append(r["id"])
    print("\n-- Register players (PUBLIC, PENDING) --")
    pid={}
    for fi,tid in enumerate(t_ids):
        pid[fi]=[]
        if not tid: continue
        for p in PLAYERS.get(fi,[]):
            r=mp_post(f"/players/register/{tid}",{k:str(v) for k,v in p["reg"].items() if v})
            if r: ok(f"Player T{tid}",r); pid[fi].append(r["id"])
            else: pid[fi].append(None)
    print("\n-- Approve all players --")
    for fi,pl in pid.items():
        for p_id in pl:
            if not p_id: continue
            r=patch(f"/players/{p_id}/approve",{},t_tok[fi])
            if r: print(f"  + Player #{p_id} APPROVED")
    print("\n-- Promote approved players -> auction pool --")
    ap={}
    for fi,pl in pid.items():
        ap[fi]=[]
        pd=PLAYERS.get(fi,[])
        for idx,p_id in enumerate(pl):
            if not p_id or idx>=len(pd): continue
            r=post(f"/players/{p_id}/add-to-auction",pd[idx]["auc"],t_tok[fi])
            if r:
                print(f"  + AP id={r['id']} {r.get('firstName','')} {r.get('lastName','')} (playerId={r.get('playerId')})")
                ap[fi].append(r["id"])
            else: ap[fi].append(None)
    print("\n-- Increment rules --")
    for fi,tid in enumerate(t_ids):
        if not tid: continue
        for rule in (SR if fi in (4,8) else DR):
            r=post(f"/tournaments/{tid}/increment-rules",rule,t_tok[fi])
            if r: print(f"  + Rule T{tid} {rule['fromAmount']}->{rule['toAmount']} +{rule['incrementBy']}")
    print("\n-- Sell / unsold --")
    def sell(fi,ap_p,tm_p,price):
        a=ap.get(fi,[]); t=team_ids.get(fi,[])
        if ap_p>=len(a) or tm_p>=len(t): return
        if not a[ap_p] or not t[tm_p]: return
        r=patch(f"/auction-players/{a[ap_p]}/sell",{"teamId":t[tm_p],"soldPrice":price},t_tok[fi])
        if r: print(f"  + Sold AP#{a[ap_p]} -> Team#{t[tm_p]} @ Rs{price:,}")
    def uns(fi,ap_p):
        a=ap.get(fi,[])
        if ap_p>=len(a) or not a[ap_p]: return
        r=patch(f"/auction-players/{a[ap_p]}/unsold",{},t_tok[fi])
        if r: print(f"  + AP#{a[ap_p]} UNSOLD")
    sell(0,0,0,75000);sell(0,1,1,150000);sell(0,2,2,100000);sell(0,3,3,120000);sell(0,4,0,125000);uns(0,6)
    sell(1,0,0,40000);sell(1,1,1,70000);sell(1,4,2,80000)
    sell(2,0,0,55000);sell(2,1,1,60000);sell(2,2,2,45000)
    sell(3,0,0,25000);sell(3,2,1,30000)
    sell(4,0,0,20000);sell(4,1,1,20000);sell(4,2,2,18000)
    sell(5,0,0,80000);sell(5,1,1,65000);sell(5,2,2,60000);sell(5,3,3,75000);sell(5,7,0,100000);uns(5,6)
    sell(6,0,0,25000);sell(6,3,1,28000)
    sell(7,1,0,30000);sell(7,2,1,27000)
    sell(8,0,0,12000);sell(8,1,1,15000);sell(8,2,2,12000)
    sell(9,0,0,70000);sell(9,3,1,60000);sell(9,5,2,45000)
    sell(10,0,0,40000);sell(10,2,1,55000);sell(10,3,2,60000);sell(10,4,3,55000)
    tp=sum(len(v) for v in pid.values()); ta=sum(len(v) for v in ap.values())
    print(f"\n=== DONE ===  Players: {tp} registered+approved  |  {ta} in auction pool  |  Pass: Admin@123")
if __name__=="__main__": main()
