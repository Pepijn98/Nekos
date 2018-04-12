import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:share/share.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:intl/intl.dart';
// import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'dart:convert';
import 'dart:async';

void main() => runApp(new MyApp());

class MyApp extends StatelessWidget {
    @override
    Widget build(BuildContext context) {
        return new MaterialApp(
            title: 'Nekos',
            theme: new ThemeData(
                primaryColor: const Color(0xFF96abec),
                accentColor: const Color(0xFF96abec),
                fontFamily: 'Nunito',
            ),
            home: new MyHomePage(title: 'Nekos Alpha App'),
        );
    }
}

class MyHomePage extends StatefulWidget {
    MyHomePage({Key key, this.title}) : super(key: key);
    final String title;

    @override
    _MyHomePageState createState() => new _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
    static const IconData LoginIcon = const IconData(0xF342, fontFamily: 'mdi');
    static const IconData LogoutIcon = const IconData(0xF343, fontFamily: 'mdi');
    static const String UserAgent = 'NekosApp/v0.0.3 (https://github.com/KurozeroPB/nekos-app)';

    final scaffoldKey = new GlobalKey<ScaffoldState>();
    final formKey = new GlobalKey<FormState>();
    final loginKey = new GlobalKey<OverlayState>();

    String _username;
    String _password;
    String _token;
    List<String> _images = [];
    Map<String, dynamic> _nekos;
    bool _nsfw = false;
    bool _timeout = false;
    bool _loginEnabled = true;
    bool _logoutEnabled = false;
    bool _profileEnabled = false;

    @override
    initState() {
        super.initState();
        _getNewNekos();

        SharedPreferences.getInstance().then((SharedPreferences prefs) {
            String token = prefs.get('token');
            if (token != null && token.isNotEmpty) {
                _loginEnabled = false;
                _logoutEnabled = true;
                _profileEnabled = true;
            }
        });
    }

    Future _submit() async {
        final form = formKey.currentState;

        if (form.validate()) {
            form.save();

            _performLogin();
        }
    }

    Future _performLogin() async {
        http.Response data = await http.post(
            'https://nekos.moe/api/v1/auth',
            headers: {
                'User-Agent': UserAgent
            },
            body: {
                'username': _username,
                'password': _password
            }
        );
        Map<String, dynamic> auth = json.decode(data.body);
        _token = auth['token'];

        SharedPreferences prefs = await SharedPreferences.getInstance();
        await prefs.setString('username', _username);
        bool valid = await prefs.setString('token', _token);
        if (valid) {
            setState(() {
                _loginEnabled = false;
                _logoutEnabled = true;
                _profileEnabled = true;
            });
        } else {
            showDialog(
                context: context,
                child: new AlertDialog(
                    title: new Text('Error', textAlign: TextAlign.center),
                    content: new Text('Something went wrong while logging in', textAlign: TextAlign.center),
                ),
            );
        }
    }

    Future _getNewNekos() async {
        if (_timeout == true) return;
        http.Response data = await http.get(
            'https://nekos.moe/api/v1/random/image?count=9&nsfw=$_nsfw',
            headers: {
                'User-Agent': UserAgent,
                'Content-Type': 'application/json'
            }
        );
        Map<String, dynamic> nekos = json.decode(data.body);

        List<String> images = nekos['images'].map((Map<String, dynamic> img) {
            return img['id'];
        }).toList();

        setState(() {
            _images = images;
            _nekos = nekos;
            _timeout = true;

            new Timer(const Duration(milliseconds: 3000), () => _timeout = false);
        });
    }

    Future _shareNeko(int index) async {
        await share('https://nekos.moe/image/${_images[index]}');
    }

    Future _openNekoInBrowser(int index) async {
        String url = 'https://nekos.moe/image/${_images[index]}';
        if (await canLaunch(url)) {
            await launch(url);
        } else {
            throw 'Could not launch $url';
        }
    }

    // ignore: unused_element
    Future _saveNeko(int index) async {
        // TODO: Figure out how to download and save image to device
        // For now one can open the image in a browser and save it
    }

    Future _imageTapped(int index) async {
        await showDialog(
            context: context,
            child: new SimpleDialog(
                children: <Widget>[
                    new Stack(
                        children: <Widget>[
                            new Container(
                                margin: new EdgeInsets.only(left: 150.0, right: 150.0, top: 150.0),
                                child: new Center(child: new CircularProgressIndicator()),
                            ),
                            new Center(
                                child: new Image.network(
                                    'https://nekos.moe/image/${_images[index]}',
                                    height: 350.0,
                                ),
                            ),
                        ],
                    ),
                    new Container(
                        padding: const EdgeInsets.only(top: 10.0),
                        child: new Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                                new Text(
                                    'Uploaded by ${_nekos['images'][index]['uploader']['username']}',
                                    style: new TextStyle(
                                        fontSize: 15.0
                                    ),
                                ),
                            ],
                        ),
                    ),
                    new Container(
                        padding: const EdgeInsets.only(top: 1.0),
                        child: new Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                                new Icon(
                                    Icons.palette,
                                    color: Colors.teal,
                                    size: 20.0,
                                ),
                                new Text(
                                    ' Artist: ${_nekos['images'][index]['artist']}',
                                    style: new TextStyle(
                                        fontSize: 15.0
                                    ),
                                ),
                            ],
                        ),
                    ),
                    new Container(
                        padding: const EdgeInsets.only(top: 1.0),
                        child: new Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                                new Icon(
                                    Icons.thumb_up,
                                    color: Colors.lightGreen,
                                    size: 20.0,
                                ),
                                new Text(
                                    ' Likes: ${_nekos['images'][index]['likes']}',
                                    style: new TextStyle(
                                        fontSize: 15.0
                                    ),
                                ),
                            ],
                        ),
                    ),
                    new Container(
                        padding: const EdgeInsets.only(top: 1.0),
                        child: new Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                                new Icon(
                                    Icons.favorite,
                                    color: Colors.red,
                                    size: 20.0,
                                ),
                                new Text(
                                    ' Favorites: ${_nekos['images'][index]['favorites']}',
                                    style: new TextStyle(
                                        fontSize: 15.0
                                    ),
                                ),
                            ],
                        ),
                    ),
                    new ButtonBar(
                        alignment: MainAxisAlignment.center,
                        children: <Widget>[
                            new MaterialButton(
                                onPressed: () => _shareNeko(index),
                                minWidth: 1.0,
                                child: new Icon(Icons.share),
                            ),
                            new MaterialButton(
                                onPressed: () => _openNekoInBrowser(index),
                                minWidth: 1.0,
                                child: new Icon(Icons.open_in_browser),
                            ),
                            new MaterialButton(
                                // onPressed: () => _saveNeko(index),
                                onPressed: null,
                                minWidth: 1.0,
                                child: new Icon(Icons.save),
                            ),
                        ],
                    ),
                ],
            ),
        );
    }

    List<Widget> _getTiles(List<String> imageList) {
        final List<Widget> tiles = <Widget>[];
        for (int i = 0; i < imageList.length; i++) {
            tiles.add(new InkResponse(
                enableFeedback: true,
                onTap: () => _imageTapped(i),
                child: new GridTile(
                    child: new Stack(
                        children: <Widget>[
                            new Center(child: new CircularProgressIndicator()),
                            new Container(
                                margin: new EdgeInsets.all(2.0),
                                decoration: new BoxDecoration(
                                    borderRadius: new BorderRadius.all(new Radius.circular(5.0)),
                                    border: new Border.all(
                                        color: const Color(0xFF96abec),
                                        width: 2.0,
                                    ),
                                    image: new DecorationImage(
                                        fit: BoxFit.cover,
                                        image: new NetworkImage('https://nekos.moe/thumbnail/${imageList[i]}')
                                    ),
                                ),
                            ),
                        ],
                    ),
                ),
            ));
        }
        return tiles;
    }

    @override
    Widget build(BuildContext context) {
        final ThemeData themeData = Theme.of(context);
        return new Scaffold(
            key: scaffoldKey,
            drawer: new Drawer(
                child: new ListView(
                    padding: EdgeInsets.zero,
                    children: <Widget>[
                        new DrawerHeader(
                            child: new Text(
                                'Options',
                                style: new TextStyle(
                                    color: Colors.white
                                ),
                            ),
                            decoration: new BoxDecoration(
                                color: themeData.primaryColor,
                            ),
                        ),
                        new ListTile(
                            key: loginKey,
                            enabled: _loginEnabled,
                            leading: new Icon(
                                LoginIcon,
                                color: themeData.primaryColor,
                            ),
                            title: new Text(
                                'Log In',
                                style: new TextStyle(
                                    color: themeData.primaryColor
                                ),
                            ),
                            onTap: () async {
                                await showDialog(
                                    context: context,
                                    child: new SimpleDialog(
                                        title: new Text(
                                            'Log In',
                                            textAlign: TextAlign.center,
                                        ),
                                        children: <Widget>[
                                            new Form(
                                                key: formKey,
                                                child: new Column(
                                                    children: <Widget>[
                                                        new Padding(
                                                            padding: new EdgeInsets.only(left: 20.0, right: 20.0, bottom: 10.0),
                                                            child: new TextFormField(
                                                                decoration: new InputDecoration(labelText: 'Username'),
                                                                validator: (String val) => val.isEmpty ? "Username can't be empty" : null,
                                                                onSaved: (String val) => _username = val,
                                                            ),
                                                        ),
                                                        new Padding(
                                                            padding: new EdgeInsets.only(left: 20.0, right: 20.0, bottom: 10.0),
                                                            child: new TextFormField(
                                                                decoration: new InputDecoration(labelText: 'Password'),
                                                                validator: (String val) => val.isEmpty ? "Password can't be empty" : null,
                                                                onSaved: (String val) => _password = val,
                                                                // obscureText: true,
                                                            ),
                                                        ),
                                                        new Padding(
                                                            padding: new EdgeInsets.only(left: 20.0, right: 20.0, bottom: 10.0),
                                                            child: new MaterialButton(
                                                                color: themeData.primaryColor,
                                                                textColor: Colors.white,
                                                                onPressed: _submit,
                                                                child: new Text('Login'),
                                                            ),
                                                        ),
                                                    ],
                                                ),
                                            ),
                                            // new Image(image: new AssetImage('assets/images/placeholder.gif')),
                                        ],
                                    ),
                                );
                            },
                        ),
                        new ListTile(
                            enabled: _logoutEnabled,
                            leading: new Icon(
                                LogoutIcon,
                                color: themeData.primaryColor,
                            ),
                            title: new Text(
                                'Log Out',
                                style: new TextStyle(
                                    color: themeData.primaryColor
                                ),
                            ),
                            onTap: () async {
                                setState(() {
                                    _loginEnabled = true;
                                    _logoutEnabled = false;
                                    _profileEnabled = false;
                                });

                                SharedPreferences prefs = await SharedPreferences.getInstance();
                                bool valid = await prefs.remove('token');
                                if (!valid) {
                                    showDialog(
                                        context: context,
                                        child: new AlertDialog(
                                            title: new Text('Error', textAlign: TextAlign.center),
                                            content: new Text('Something went wrong while logging out', textAlign: TextAlign.center),
                                        ),
                                    );
                                }
                            },
                        ),
                        new ListTile(
                            leading: new Icon(
                                Icons.file_upload,
                                color: themeData.primaryColor,
                            ),
                            title: new Text(
                                'Upload a neko',
                                style: new TextStyle(
                                    color: themeData.primaryColor
                                ),
                            ),
                            onTap: () async {
                                await showDialog(
                                    context: context,
                                    child: new SimpleDialog(
                                        title: new Text(
                                            'Soon™',
                                            textAlign: TextAlign.center,
                                        ),
                                        children: <Widget>[
                                            new Image(image: new AssetImage('assets/images/placeholder.gif')),
                                        ],
                                    ),
                                );
                            },
                        ),
                        new ListTile(
                            enabled: _profileEnabled,
                            leading: new Icon(
                                Icons.account_box,
                                color: themeData.primaryColor,
                            ),
                            title: new Text(
                                'Profile',
                                style: new TextStyle(
                                    color: themeData.primaryColor
                                ),
                            ),
                            onTap: () async {
                                http.Response resp = await http.post(
                                    'https://nekos.moe/api/v1/users/search',
                                    headers: {
                                        'User-Agent': UserAgent
                                    },
                                    body: {
                                        'query': _username
                                    }
                                );
                                Map<String, dynamic> data = json.decode(resp.body);
                                Map<String, dynamic> profile = data['users'][0];

                                DateFormat formatter = new DateFormat('hh:mm:ss dd-MM-yyyy');

                                await showDialog(
                                    context: context,
                                    child: new SimpleDialog(
                                        title: new Text(
                                            'Username: ${profile['username']}',
                                            textAlign: TextAlign.center,
                                        ),
                                        children: <Widget>[
                                            new Text(
                                                'Created on: ${formatter.format(DateTime.parse(profile['createdAt']))}',
                                                textAlign: TextAlign.center,
                                            ),
                                            new Image(image: new AssetImage('assets/images/placeholder.gif')),
                                        ],
                                    ),
                                );
                            },
                        ),
                    ],
                ),
            ),
            appBar: new AppBar(
                centerTitle: true,
                title: new Text(
                    widget.title,
                    textAlign: TextAlign.center,
                    style: new TextStyle(color: Colors.white),
                ),
                actions: <Widget>[
                    new Row(
                        children: <Widget>[
                            new Text(
                                'nsfw',
                                style: new TextStyle(color: Colors.white),
                            ),
                            new Switch(
                                value: _nsfw,
                                activeColor: Colors.white,
                                activeTrackColor: Colors.lightGreen,
                                onChanged: (bool val) {
                                    setState(() => _nsfw = val);
                                    _getNewNekos();
                                },
                            ),
                        ],
                    ),
                ],
            ),
            body: new Container(
                decoration: new BoxDecoration(
                    image: new DecorationImage(
                        image: new AssetImage('assets/images/background.png'),
                        fit: BoxFit.none,
                        repeat: ImageRepeat.repeat,
                    ),
                ),
                child: new GridView.count(
                    crossAxisCount: 3,
                    padding: const EdgeInsets.only(top: 10.0),
                    mainAxisSpacing: 50.0,
                    children: _getTiles(_images),
                ),
            ),
            floatingActionButton: new FloatingActionButton(
                tooltip: 'Get New Images',
                onPressed: _getNewNekos,
                child: new Icon(
                    Icons.refresh,
                    color: Colors.white,
                ),
            ),
        );
    }
}
