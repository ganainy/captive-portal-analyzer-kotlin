package com.example.captive_portal_analyzer_kotlin.screens.about

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.captive_portal_analyzer_kotlin.R
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
                    append(stringResource(R.string.we_wanted_to_create_a_database_of_different_captive_portals_and_which_data_they_collect_from_users_and_what_privacy_rules_they_have_to_help_in_the_research_field_of_privacy_and_security_of_wlan_networks_in_the))
                    pushStringAnnotation(tag = "if-is", annotation = "https://internet-sicherheit.de/ueber-uns/das-institut/")
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
                question = stringResource(R.string.how_to_create_a_good_report),
                answer = buildAnnotatedString {
                    append(stringResource(R.string._1_connect_to_a_network_with_a_captive_portal))
                    append(stringResource(R.string._2_interact_with_as_many_pages_as_possible_within_the_captive_login_site_inside_the_app))
                    append(stringResource(R.string._3_view_the_captive_portal_privacy_policy_and_terms_of_service_tos_if_found))
                    append(stringResource(R.string._4_complete_login_to_the_captive_portal))
                    append(stringResource(R.string._5_review_and_upload_the_session_data_for_more_analysis))
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
 * A composable function that displays a Question and Answer pair.
 *
 * @param question The text displayed as the question.
 * @param answer The text displayed as the answer.
 * */
@Composable
fun FAQItem(question: String, answer: AnnotatedString) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        ClickableText(
            text = answer,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 20.sp,
                color =  MaterialTheme.colorScheme.onSurface
            ),
            onClick = { offset ->
                answer.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        openExternalWebsite(annotation.item, context)
                    }
            }
        )
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

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun FAQItemPreview() {
    AppTheme {
        FAQItem(
            question = "Sample Question?",
            answer = buildAnnotatedString {
                append("Sample Answer with a link to ")
                pushStringAnnotation(tag = "if-is", annotation = "https://example.com")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("example.com")
                }
                pop()
            }
        )
    }
}



