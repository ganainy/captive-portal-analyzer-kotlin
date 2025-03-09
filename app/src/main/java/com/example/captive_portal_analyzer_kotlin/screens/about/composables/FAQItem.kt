package com.example.captive_portal_analyzer_kotlin.screens.about.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.captive_portal_analyzer_kotlin.screens.about.openExternalWebsite
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

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
