package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KernelModulesTest {
    @Test
    fun lastCommaSeparatesSelectedModulesFromSearch() {
        assertTrue(BlockModulesInput("oplus").selectedModules.isEmpty())
        assertEquals("oplus", BlockModulesInput("oplus").searchQuery)
        assertEquals(listOf("mkp"), BlockModulesInput("mkp,oplus").selectedModules)
        assertEquals("oplus", BlockModulesInput("mkp,oplus").searchQuery)
        assertEquals(listOf("mkp", "vr"), BlockModulesInput(" mkp, , vr, mkp, oplus ").selectedModules)
        assertEquals("oplus", BlockModulesInput(" mkp, , vr, mkp, oplus ").searchQuery)
        assertEquals("", BlockModulesInput("mkp,").searchQuery)
    }

    @Test
    fun searchMatchesSubstringsWithoutCaseSensitivity() {
        val state = KernelModuleState(
            loaded = listOf("mkp", "oplus_secure_guard", "vendor_oplus_driver", "zram"),
        )
        val input = BlockModulesInput("mkp,oplus")
        assertEquals(
            listOf("oplus_secure_guard", "vendor_oplus_driver"),
            state.availableModules(input.selectedModules, input.searchQuery),
        )
        assertEquals(
            listOf("oplus_secure_guard", "vendor_oplus_driver"),
            state.availableModules(emptyList(), "OPLUS"),
        )
        assertTrue(state.availableModules(emptyList(), "nonexistent").isEmpty())
    }

    @Test
    fun selectedMatchesAppearBeforeOtherMatches() {
        val state = KernelModuleState(loaded = listOf("a_oplus", "mkp", "z_oplus"))
        assertEquals(
            listOf("z_oplus", "a_oplus"),
            state.availableModules(listOf("mkp", "z_oplus"), "oplus"),
        )
        assertEquals(
            listOf("mkp", "z_oplus", "a_oplus"),
            state.availableModules(listOf("z_oplus", "mkp")),
        )
    }

    @Test
    fun selectingSearchResultReplacesQueryAndKeepsModuleSelected() {
        val checked = BlockModulesInput("oplus").toggle("oplus_secure_guard_new", true)
        assertEquals("oplus_secure_guard_new", checked.text)
        assertEquals(listOf("oplus_secure_guard_new"), checked.selectedModules)
        assertEquals("", checked.searchQuery)
        assertEquals(checked, checked.edit(checked.text))
        assertEquals(checked, checked.toggle("oplus_secure_guard_new", true))
        assertEquals("", checked.toggle("oplus_secure_guard_new", false).text)
        assertTrue(checked.toggle("oplus_secure_guard_new", false).selectedModules.isEmpty())
    }

    @Test
    fun selectingAnotherResultPreservesPreviousSelectionsAndClearsQuery() {
        val checked = BlockModulesInput("mkp,oplus").toggle("oplus_secure_guard_new", true)
        assertEquals("mkp,oplus_secure_guard_new", checked.text)
        assertEquals(listOf("mkp", "oplus_secure_guard_new"), checked.selectedModules)
        assertEquals("", checked.searchQuery)

        val searching = checked.edit(checked.text + ",zram")
        assertEquals(checked.selectedModules, searching.selectedModules)
        assertEquals("zram", searching.searchQuery)
        val selected = searching.toggle("zram", true)
        assertEquals("mkp,oplus_secure_guard_new,zram", selected.text)
        assertEquals(listOf("mkp", "oplus_secure_guard_new", "zram"), selected.selectedModules)
        assertEquals("", selected.searchQuery)
        assertEquals("mkp,zram", selected.toggle("oplus_secure_guard_new", false).text)
        assertEquals(listOf("oplus_secure_guard_new"), checked.toggle("mkp", false).selectedModules)
    }

    @Test
    fun currentBlacklistIsConfirmedWithoutTrailingComma() {
        val input = BlockModulesInput.fromSelected(listOf("mkp", "oplus_secure_guard_new"))
        assertEquals("mkp,oplus_secure_guard_new", input.text)
        assertEquals(listOf("mkp", "oplus_secure_guard_new"), input.selectedModules)
        assertEquals("", input.searchQuery)
        assertEquals(listOf("mkp"), input.toggle("oplus_secure_guard_new", false).selectedModules)
        assertTrue(input.edit("").selectedModules.isEmpty())
    }

    @Test
    fun manualNamesMustBeConfirmedAndSearchNeverBecomesABlockArgument() {
        assertTrue(BlockModulesInput("custom_driver").selectedModules.isEmpty())
        assertEquals(listOf("custom_driver"), BlockModulesInput("custom_driver,").selectedModules)
        assertEquals(
            listOf("custom_driver"),
            KernelModuleState().availableModules(BlockModulesInput("custom_driver,").selectedModules),
        )
        assertEquals(listOf("mkp"), BlockModulesInput("mkp,oplus;reboot").selectedModules)
        assertEquals(listOf("mkp"), BlockModulesInput("mkp,oplus").selectedModules)
        assertEquals("mkp", BlockModulesInput().toggle("mkp", true).text)
        assertEquals("", BlockModulesInput("mkp,").toggle("mkp", false).text)
    }

    @Test
    fun blockedModulesHandleNativeEmptyValueAndCommaSeparatedNames() {
        assertTrue(parseBlockedKernelModules("(null)\n").isEmpty())
        assertTrue(parseBlockedKernelModules("\n").isEmpty())
        assertEquals(
            listOf("mkp", "oplus_secure_guard"),
            parseBlockedKernelModules("oplus_secure_guard,mkp,oplus_secure_guard\n"),
        )
    }

    @Test
    fun blockedAndManualModulesRemainAvailableWhenNotLoaded() {
        val state = KernelModuleState(
            loaded = listOf("zram"),
            blocked = listOf("oplus_secure_guard"),
        )
        assertEquals(
            listOf("custom_driver", "oplus_secure_guard", "zram"),
            state.availableModules(listOf("custom_driver", "oplus_secure_guard")),
        )
        // Unchecking an already blocked module must leave it available to reselect.
        assertEquals(listOf("oplus_secure_guard", "zram"), state.availableModules(emptyList()))
    }

    @Test
    fun blockedModulesRemainAvailableIfProcModulesCannotBeRead() {
        assertEquals(
            listOf("mkp"),
            KernelModuleState(blocked = listOf("mkp")).availableModules(emptyList()),
        )
        assertEquals(
            listOf("custom_driver"),
            KernelModuleState().availableModules(listOf("custom_driver")),
        )
    }

    @Test
    fun loadedModulesIncludeAllNamesRegardlessOfVendor() {
        assertEquals(
            listOf("custom_driver", "mkp", "oplus_secure_guard", "zram"),
            parseLoadedKernelModules(
                listOf(
                    "zram 32768 0 - Live 0x00000000",
                    "  custom_driver\t16384\t1\tzram,\tLive\t0x00000000",
                    "mkp 16384 0 - Live 0x00000000",
                    "oplus_secure_guard 16384 0 - Live 0x00000000",
                    "zram 32768 0 - Live 0x00000000",
                )
            ),
        )
    }

    @Test
    fun loadedModulesIgnoreBlankAndUnusableNames() {
        assertEquals(
            listOf("valid_module"),
            parseLoadedKernelModules(listOf("", " \t", "bad;name 0", "bad-name 0", "a".repeat(65), "valid_module 0")),
        )
        assertTrue(parseLoadedKernelModules(emptyList()).isEmpty())
    }

    @Test
    fun manualListAllowsWhitespaceAndRejectsShellSyntax() {
        assertEquals(listOf("mkp", "custom_driver"), parseBlockModules(" mkp, , custom_driver ,"))
        assertTrue(isBlockModulesValid(" mkp, , custom_driver ,"))
        assertTrue(isBlockModulesValid(""))
        assertTrue(isBlockModulesValid("Driver_123"))
        listOf("bad-name", "mkp;reboot", "mkp\nreboot", "$(reboot)", "a".repeat(65)).forEach {
            assertFalse(it, isBlockModulesValid(it))
        }
    }

    @Test
    fun blockListRespectsKernelBufferLimitAfterTrimming() {
        val maxLengthList = List(4) { ('a' + it).toString().repeat(63) }.joinToString(",")
        assertEquals(255, maxLengthList.length)
        assertTrue(isBlockModulesValid(maxLengthList))
        assertTrue(isBlockModulesValid(" $maxLengthList , "))
        assertFalse(isBlockModulesValid(maxLengthList + "d"))
    }
}
