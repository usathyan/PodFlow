class Podcast {
  final int? id;
  final String title;
  final String description;
  final String feedUrl;
  final String? imageUrl;
  final String? author;
  final String? link;
  final DateTime? lastUpdated;
  final DateTime subscribedAt;

  Podcast({
    this.id,
    required this.title,
    required this.description,
    required this.feedUrl,
    this.imageUrl,
    this.author,
    this.link,
    this.lastUpdated,
    DateTime? subscribedAt,
  }) : subscribedAt = subscribedAt ?? DateTime.now();

  Podcast copyWith({
    int? id,
    String? title,
    String? description,
    String? feedUrl,
    String? imageUrl,
    String? author,
    String? link,
    DateTime? lastUpdated,
    DateTime? subscribedAt,
  }) {
    return Podcast(
      id: id ?? this.id,
      title: title ?? this.title,
      description: description ?? this.description,
      feedUrl: feedUrl ?? this.feedUrl,
      imageUrl: imageUrl ?? this.imageUrl,
      author: author ?? this.author,
      link: link ?? this.link,
      lastUpdated: lastUpdated ?? this.lastUpdated,
      subscribedAt: subscribedAt ?? this.subscribedAt,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'title': title,
      'description': description,
      'feedUrl': feedUrl,
      'imageUrl': imageUrl,
      'author': author,
      'link': link,
      'lastUpdated': lastUpdated?.toIso8601String(),
      'subscribedAt': subscribedAt.toIso8601String(),
    };
  }

  factory Podcast.fromMap(Map<String, dynamic> map) {
    return Podcast(
      id: map['id'] as int?,
      title: map['title'] as String,
      description: map['description'] as String,
      feedUrl: map['feedUrl'] as String,
      imageUrl: map['imageUrl'] as String?,
      author: map['author'] as String?,
      link: map['link'] as String?,
      lastUpdated: map['lastUpdated'] != null
          ? DateTime.parse(map['lastUpdated'] as String)
          : null,
      subscribedAt: DateTime.parse(map['subscribedAt'] as String),
    );
  }
}
