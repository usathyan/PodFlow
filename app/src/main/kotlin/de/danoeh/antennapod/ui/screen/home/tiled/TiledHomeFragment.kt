package de.danoeh.antennapod.ui.screen.home.tiled

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import de.danoeh.antennapod.ui.screen.home.carousel.CarouselHomeScreen
import de.danoeh.antennapod.ui.theme.PodFlowTheme

/**
 * Fragment wrapper for the Compose-based Home screen.
 * Displays the horizontal carousel of podcasts for commute radio experience.
 */
class TiledHomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PodFlowTheme {
                    CarouselHomeScreen(
                        onPodcastClick = { podcast ->
                            // Navigate to podcast details when tile is clicked (not play button)
                            // This can be extended to load the feed details fragment
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "TiledHomeFragment"

        fun newInstance(): TiledHomeFragment {
            return TiledHomeFragment()
        }
    }
}
