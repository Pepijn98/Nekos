import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:share/share.dart';
import 'dart:convert';
import 'dart:async';

void main() => runApp(new MyApp());

class MyApp extends StatelessWidget {
    @override
    Widget build(BuildContext context) {
        return new MaterialApp(
            title: 'Nekos',
            theme: new ThemeData(
                primarySwatch: const MaterialColor(0xFF96abec, const {
                    50: const Color(0xFF96abec),
                    100: const Color(0xFF96abec),
                    200: const Color(0xFF96abec),
                    300: const Color(0xFF96abec),
                    400: const Color(0xFF96abec),
                    500: const Color(0xFF96abec),
                    600: const Color(0xFF96abec),
                    700: const Color(0xFF96abec),
                    800: const Color(0xFF96abec),
                    900: const Color(0xFF96abec)
                }),
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
    List<String> _images = [];
    Map<String, dynamic> _nekos;
    bool _nsfw = false;

    @override
    initState() {
        super.initState();
        _getNewNekos();
    }

    Future _getNewNekos() async {
        http.Response data = await http.get('https://nekos.moe/api/v1/random/image?count=9&nsfw=$_nsfw', headers: {
            'User-Agent':
            'NekosApp/v0.0.2 (https://github.com/KurozeroPB/nekos-app)'
        });
        Map<String, dynamic> nekos = json.decode(data.body);

        List<String> images = nekos['images'].map((Map<String, dynamic> img) {
            return img['id'];
        }).toList();

        setState(() {
            _images = images;
            _nekos = nekos;
        });
    }

    Future _saveNeko(int index) async {
        // TODO: Figure out how to download and save image to device
    }

    Future _shareNeko(int index) async {
        await share('https://nekos.moe/image/${_images[index]}');
    }

    Future _imageTapped(int index) async {
        await showDialog(
            context: context,
            child: new SimpleDialog(
                title: new Text('Artist: ${_nekos['images'][index]['artist']}'),
                children: <Widget>[
                    new Stack(
                        children: <Widget>[
                            new Container(
                                margin: new EdgeInsets.all(100.0),
                                child: new Center(child: new CircularProgressIndicator()),
                            ),
                            new Center(
                                child: new Image.network(
                                    'https://nekos.moe/image/${_images[index]}',
                                    height: 250.0,
                                ),
                            ),
                        ],
                    ),
                    new Center(
                        child: new Text(
                            'Likes: **  '
                            'Favorites: **\n'
                            'Uploader: ******'
                        ),
                    ),
                    new ButtonBar(
                        alignment: MainAxisAlignment.center,
                        children: <Widget>[
                            new MaterialButton(
                                onPressed: () => _shareNeko(index),
                                child: new Icon(Icons.share),
                            ),
                            new MaterialButton(
                                onPressed: () => _saveNeko(index),
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
        return new Scaffold(
            appBar: new AppBar(
                title: new Text(
                    widget.title,
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
                tooltip: 'Next page',
                onPressed: _getNewNekos,
                child: new Icon(
                    Icons.arrow_right,
                    color: Colors.white,
                ),
            ),
        );
    }
}
