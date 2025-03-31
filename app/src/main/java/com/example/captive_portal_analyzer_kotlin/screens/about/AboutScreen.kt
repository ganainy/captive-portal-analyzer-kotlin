package com.example.captive_portal_analyzer_kotlin.screens.about

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.screens.about.composables.FAQItem
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * Simple screen that shows a list of common questions and answers that can help users understand
 * how the app works.
 * */
@Composable
fun AboutScreen() {

    Scaffold(

    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FAQItem(
                question = stringResource(R.string.why_was_this_app_created),
                answer = buildAnnotatedString {
                    append(stringResource(R.string.project_goal_captive_portals_revised))
                    pushStringAnnotation(tag = "URL", annotation = "https://internet-sicherheit.de/ueber-uns/das-institut/")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("if(is) institute Gelsenkirchen")
                    }
                    pop()
                }
            )


            FAQItem(
                // Use stringResource(R.string.how_the_app_works) if you have it defined
                question = stringResource(R.string.how_does_the_app_use_customwebview_for_network_analysis),
                answer = buildAnnotatedString {
                    append(stringResource(R.string.this_app_utilizes_the_customwebview_library_an_advanced_webview_component))
                    append(stringResource(R.string.it_allows_the_app_to_intercept_inspect_and_analyze_the_http_s_network_requests_and_responses_specifically_generated_by_the_web_content_loaded_within_the_app_s_integrated_browser))
                    append(stringResource(R.string.this_capability_is_crucial_for_understanding_the_network_behavior_of_visited_web_pages_directly_within_the_app_learn_more_about_the_library_here))

                    // Link to CustomWebView repo
                    pushStringAnnotation(tag = "URL", annotation = "https://github.com/vknow360/CustomWebView")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("https://github.com/vknow360/CustomWebView")
                    }
                    pop()
                    append(".") // Added punctuation
                }
            )

            FAQItem(
                // Use stringResource(R.string.why_the_app_uses_pcapdroid) if you have it defined
                question = stringResource(R.string.why_does_the_app_use_pcapdroid),
                answer = buildAnnotatedString {
                    append(stringResource(R.string.to_perform_comprehensive_network_analysis_the_app_needs_to_capture_raw_network_packets))
                    append(stringResource(R.string.it_leverages_functionality_based_on_pcapdroid_which_enables_network_traffic_capture_on_android_devices))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { // Emphasize non-root
                        append(stringResource(R.string.without_requiring_root_access))
                    }
                    append(stringResource(R.string.pcapdroid_achieves_this_by_utilizing_android_s_vpnservice_api_to_create_a_local_vpn_tunnel_intercepting_network_packets_for_analysis_before_they_leave_the_device))
                    append(stringResource(R.string.this_allows_the_app_to_provide_deep_packet_inspection_capabilities_on_standard_non_rooted_phones_find_more_details_about_pcapdroid_here))

                    // Link to PCAPdroid repo
                    pushStringAnnotation(tag = "URL", annotation = "https://github.com/emanuele-f/PCAPdroid")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("https://github.com/emanuele-f/PCAPdroid")
                    }
                    pop()
                    append(".") // Added punctuation
                }
            )

            FAQItem(
                question = stringResource(R.string.how_to_create_a_good_report),
                answer = buildAnnotatedString {
                    append(stringResource(R.string._1_connect_to_a_network_with_a_captive_portal))
                    append(stringResource(R.string._3_interact_with_as_many_pages_as_possible_within_the_captive_login_site_inside_the_app))
                    append(stringResource(R.string._4_view_the_captive_portal_privacy_policy_and_terms_of_service_tos_if_found))
                    append(stringResource(R.string._5_complete_login_to_the_captive_portal))
                    append(stringResource(R.string._6_review_and_upload_the_session_data_for_more_analysis))
                }
            )

            FAQItem(
                question = stringResource(R.string.what_data_will_i_share_with_you),
                answer = buildAnnotatedString {
                    append(stringResource(R.string.after_you_successfully_create_a_session_it_is_only_stored_on_your_device_and_won_t_be_shared_with_us_until_you_review_the_content_and_approve_the_upload_from_the_sessions_page))
                }
            )





        }
    }
}

/**
 * Opens a website in the default browser.
 *
 * @param url The website to open.
 * @param context The context of the calling Activity.
 * */
fun openExternalWebsite(url: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}


@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun AboutScreenPreview() {
    AppTheme {
        AboutScreen()
    }
}



