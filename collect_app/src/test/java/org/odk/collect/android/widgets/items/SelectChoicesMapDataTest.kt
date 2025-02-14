package org.odk.collect.android.widgets.items

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.odk.collect.android.support.MockFormEntryPromptBuilder
import org.odk.collect.android.widgets.support.FormFixtures.selectChoice
import org.odk.collect.android.widgets.support.FormFixtures.treeElement
import org.odk.collect.geo.selection.MappableSelectItem.IconifiedText

@RunWith(AndroidJUnit4::class)
class SelectChoicesMapDataTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `choices without geometry are not included in mappable items`() {
        val choices = listOf(
            selectChoice(
                value = "a",
                item = treeElement(children = listOf(treeElement("geometry", "12.0 -1.0 305 0")))
            ),
            selectChoice(
                value = "b",
                item = treeElement(children = emptyList())
            )
        )

        val prompt = MockFormEntryPromptBuilder()
            .withLongText("Which is your favourite place?")
            .withSelectChoices(choices)
            .withSelectChoiceText(mapOf(choices[0] to "A", choices[1] to "B"))
            .build()

        val resources = ApplicationProvider.getApplicationContext<Application>().resources
        val data = SelectChoicesMapData(resources, prompt)
        assertThat(data.getItemCount().value, equalTo(2))
        assertThat(data.getMappableItems().value.size, equalTo(1))
        assertThat(data.getMappableItems().value[0].name, equalTo("A"))
    }

    @Test
    fun `additional children are returned as properties`() {
        val choices = listOf(
            selectChoice(
                value = "a",
                item = treeElement(
                    children = listOf(
                        treeElement("geometry", "12.0 -1.0 305 0"),
                        treeElement("property", "blah")
                    )
                )
            )
        )

        val prompt = MockFormEntryPromptBuilder()
            .withLongText("Which is your favourite place?")
            .withSelectChoices(choices)
            .withSelectChoiceText(mapOf(choices[0] to "A"))
            .build()

        val resources = ApplicationProvider.getApplicationContext<Application>().resources
        val data = SelectChoicesMapData(resources, prompt)

        val properties = data.getMappableItems().value[0].properties
        assertThat(properties.size, equalTo(1))
        assertThat(properties[0], equalTo(IconifiedText(null, "property: blah")))
    }
}
