package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StockPhotoParserTest {

    @Test
    fun parseWikipediaPages_filtersSvgAndSortsByIndex() {
        val json = """
            {
              "query": {
                "pages": {
                  "2": {
                    "index": 2,
                    "title": "Tabby cat",
                    "thumbnail": { "source": "https://upload.wikimedia.org/tabby.jpg" }
                  },
                  "1": {
                    "index": 1,
                    "title": "Cat",
                    "thumbnail": { "source": "https://upload.wikimedia.org/cat.jpg" }
                  },
                  "3": {
                    "index": 3,
                    "title": "Vector Icon",
                    "thumbnail": { "source": "https://upload.wikimedia.org/icon.svg" }
                  }
                }
              }
            }
        """.trimIndent()

        val results = StockPhotoRepository.parseWikipediaPages(json)
        assertEquals(2, results.size)
        assertEquals("Cat", results[0].title)
        assertEquals("https://upload.wikimedia.org/cat.jpg", results[0].thumbUrl)
        assertEquals("Tabby cat", results[1].title)
    }

    @Test
    fun parseCommonsPages_extractsBitmapThumbAndLicense() {
        val json = """
            {
              "query": {
                "pages": {
                  "10": {
                    "index": 1,
                    "title": "File:Red_Apple_Fruit.jpg",
                    "imageinfo": [
                      {
                        "mime": "image/jpeg",
                        "thumburl": "https://upload.wikimedia.org/thumb/red_apple.jpg",
                        "extmetadata": {
                          "LicenseShortName": { "value": "CC BY-SA 4.0" }
                        }
                      }
                    ]
                  },
                  "11": {
                    "index": 2,
                    "title": "File:Document.pdf",
                    "imageinfo": [
                      {
                        "mime": "application/pdf",
                        "thumburl": "https://upload.wikimedia.org/doc.pdf"
                      }
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val results = StockPhotoRepository.parseCommonsPages(json)
        assertEquals(1, results.size)
        assertEquals("Red Apple Fruit", results[0].title)
        assertEquals("CC BY-SA 4.0", results[0].license)
        assertEquals("Wikimedia Commons", results[0].sourceName)
    }

    @Test
    fun parseOpenverseResults_extractsBitmapsFiltersSvgAndSetsFallback() {
        val json = """
            {
              "results": [
                {
                  "id": "abc-123",
                  "title": "Cute Kitten",
                  "filetype": "jpg",
                  "thumbnail": "https://api.openverse.org/v1/images/abc-123/thumb/",
                  "url": "https://example.org/kitten.jpg",
                  "source": "flickr",
                  "license": "by"
                },
                {
                  "id": "svg-456",
                  "title": "Vector Dog",
                  "filetype": "svg",
                  "thumbnail": "https://example.org/dog.svg",
                  "url": "https://example.org/dog.svg",
                  "source": "wikimedia",
                  "license": "cc0"
                },
                {
                  "id": "full-only",
                  "title": "",
                  "filetype": "png",
                  "thumbnail": "",
                  "url": "https://example.org/illustration.png",
                  "provider": "stocksnap",
                  "license": ""
                }
              ]
            }
        """.trimIndent()

        val results = StockPhotoRepository.parseOpenverseResults(json)
        assertEquals(2, results.size)

        assertEquals("ov_abc-123", results[0].id)
        assertEquals("Cute Kitten", results[0].title)
        assertEquals("https://api.openverse.org/v1/images/abc-123/thumb/", results[0].thumbUrl)
        assertEquals("https://example.org/kitten.jpg", results[0].fallbackUrl)
        assertEquals("Openverse • Flickr", results[0].sourceName)
        assertEquals("BY", results[0].license)

        assertEquals("ov_full-only", results[1].id)
        assertEquals("Obrázek", results[1].title)
        assertEquals("https://example.org/illustration.png", results[1].thumbUrl)
        assertEquals("", results[1].fallbackUrl)
        assertEquals("Openverse • Stocksnap", results[1].sourceName)
        assertEquals("CC", results[1].license)
    }

    @Test
    fun parseIconifyResults_buildsSvgUrlsAndCollectionLabels() {
        val json = """
            {
              "icons": [
                "fluent-emoji-flat:red-apple",
                "icon-park:sweater",
                "invalid-no-colon"
              ],
              "collections": {
                "fluent-emoji-flat": {
                  "name": "Fluent Emoji Flat",
                  "license": { "title": "MIT" }
                },
                "icon-park": {
                  "name": "IconPark",
                  "license": { "title": "Apache 2.0" }
                }
              }
            }
        """.trimIndent()

        val results = StockPhotoRepository.parseIconifyResults(json)
        assertEquals(2, results.size)

        assertEquals("iconify_fluent-emoji-flat:red-apple", results[0].id)
        assertEquals("Red apple", results[0].title)
        assertEquals("https://api.iconify.design/fluent-emoji-flat/red-apple.svg", results[0].thumbUrl)
        assertEquals("Iconify • Fluent Emoji Flat", results[0].sourceName)
        assertEquals("MIT", results[0].license)

        assertEquals("iconify_icon-park:sweater", results[1].id)
        assertEquals("Sweater", results[1].title)
        assertEquals("https://api.iconify.design/icon-park/sweater.svg", results[1].thumbUrl)
        assertEquals("Iconify • IconPark", results[1].sourceName)
        assertEquals("Apache 2.0", results[1].license)
    }
}
