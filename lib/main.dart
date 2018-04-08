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

  @override
  initState() {
    super.initState();
    _getNewNekos();
  }

  Future _getNewNekos() async {
    http.Response data = await http.get('https://nekos.moe/api/v1/random/image?count=12&nsfw=false');
    Map<String, dynamic> nekos = json.decode(data.body);

    List<String> images = nekos['images'].map((Map<String, dynamic> img) {
      return 'https://nekos.moe/image/' + img['id'];
    }).toList();

    setState(() {
      _images = images;
      _nekos = nekos;
    });
  }

  Future _saveNeko(int index) async {

  }

  Future _shareNeko(int index) async {
    await share(_images[index]);
  }

  Future _imageTapped(int index) async {
    await showDialog(
      context: context,
      child: new SimpleDialog(
        title: new Text('Artist: ${_nekos['images'][index]['artist']}'),
        children: <Widget>[
          new Image.network(_images[index]),
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
      tiles.add(new GridTile(
        child: new InkResponse(
          enableFeedback: true,
          onTap: () => _imageTapped(i),
          child: new Stack(
            children: <Widget>[
              new Center(child: new CircularProgressIndicator()),
              new Center(child: new Image.network(imageList[i], fit: BoxFit.cover)),
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
      ),
      body: new Center(
        child: new GridView.count(
          crossAxisCount: 3,
          childAspectRatio: 1.0,
          padding: const EdgeInsets.only(top: 10.0),
          mainAxisSpacing: 30.0,
          crossAxisSpacing: 10.0,
          children: _getTiles(_images),
        ),
      ),
      floatingActionButton: new FloatingActionButton(
        tooltip: 'Next page',
        onPressed: _getNewNekos,
        child: new Icon(Icons.arrow_right),
      ),
    );
  }
}
