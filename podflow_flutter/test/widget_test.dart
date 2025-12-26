import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:podflow/main.dart';

void main() {
  testWidgets('PodFlow app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: PodFlowApp()));

    // Verify app title is displayed
    expect(find.text('PodFlow'), findsOneWidget);

    // Verify bottom navigation exists
    expect(find.text('Subscriptions'), findsOneWidget);
    expect(find.text('Discover'), findsOneWidget);
    expect(find.text('Downloads'), findsOneWidget);
    expect(find.text('Settings'), findsOneWidget);

    // Verify initial screen shows subscriptions placeholder
    expect(find.text('Your Subscriptions'), findsOneWidget);
  });
}
