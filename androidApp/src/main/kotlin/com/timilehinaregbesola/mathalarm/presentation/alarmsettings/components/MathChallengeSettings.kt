package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.buildQuestionString
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.generateChallengeProblems
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.EmojiSymbols
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.KeyboardArrowDown
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import kotlin.random.Random

/** Compact summary backed by the alarm editor view model. */
@Composable
internal fun MathChallengeSettings(
    challenge: MathChallenge,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val names = challengePresetNames
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = ChallengeDimensions.ROW_SPACING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        SettingsLeadingIcon(EmojiSymbols)
        Column(Modifier.weight(1f)) {
            Text(strings.mathChallengeTitle, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = strings.challengeSummary(
                    if (challenge.difficultyMix.isNotEmpty()) strings.mixedDifficulty
                    else names[challenge.normalized().difficulty],
                    challenge.questionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onEdit,
            modifier = Modifier.sizeIn(minWidth = ChallengeDimensions.MIN_TOUCH_TARGET, minHeight = ChallengeDimensions.MIN_TOUCH_TARGET),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(
                strings.edit,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Composable
internal fun ChallengeEditor(
    initial: MathChallenge,
    onDismiss: () -> Unit,
    onApply: (MathChallenge) -> Unit,
) {
    var preset by rememberSaveable { mutableStateOf(initial.difficulty) }
    var countText by rememberSaveable { mutableStateOf(initial.questionCount.toString()) }
    var operations by rememberSaveable { mutableStateOf(initial.operations) }
    var addition by rememberSaveable { mutableStateOf(initial.additionRange) }
    var factors by rememberSaveable { mutableStateOf(initial.factorRange) }
    var seed by rememberSaveable { mutableStateOf(0) }
    var usedCustom by rememberSaveable { mutableStateOf(initial.difficulty == MathChallenge.CUSTOM) }
    var mixing by rememberSaveable { mutableStateOf(initial.difficultyMix.isNotEmpty()) }
    var mix by rememberSaveable { mutableStateOf(initial.difficultyMix) }
    val count = if (mixing) mix.length else countText.toIntOrNull()
    val valid = count != null && count in 1..MathChallenge.MAX_QUESTIONS
    val draft = MathChallenge(
        difficulty = preset,
        questionCount = count ?: 1,
        operations = operations,
        additionRange = addition,
        factorRange = factors,
        difficultyMix = if (mixing) mix else "",
    )
    val actionColors =
        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ChallengeDimensions.MAX_CONTENT_WIDTH).fillMaxSize().padding(
                    start = MaterialTheme.spacing.extraMedium,
                    end = MaterialTheme.spacing.extraMedium,
                    top = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.extraMedium,
                ),
            ) {
                Text(
                    strings.mathChallengeTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = MaterialTheme.spacing.large,
                            start = MaterialTheme.spacing.medium,
                            end = MaterialTheme.spacing.medium,
                            bottom = MaterialTheme.spacing.medium,
                        ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = ChallengeDimensions.MIN_CONTROLS_HEIGHT),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                    ) {
                        if (mixing) {
                            Text(
                                strings.mixedQuestionTotal(mix.length, MathChallenge.MAX_QUESTIONS),
                                style = MaterialTheme.typography.labelLarge
                            )
                            MixedDifficultyCounts(
                                mix = mix,
                                onMixChange = { mix = it }
                            )
                        } else {
                            DifficultyChoices(
                                selected = preset,
                                onSelect = { selected ->
                                    if (selected == MathChallenge.CUSTOM && !usedCustom) {
                                        addition = preset
                                        factors = preset
                                        usedCustom = true
                                    }
                                    preset = selected
                                }
                            )
                            QuestionCountControl(
                                countText = countText,
                                onCountChange = { countText = it })
                        }
                    }
                    if (mixing || (preset != MathChallenge.CUSTOM && (count ?: 0) > 1)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = mixing,
                                    role = Role.Checkbox,
                                    onValueChange = { enabled ->
                                        if (enabled) mix = preset.toString().repeat(count ?: 1)
                                        else {
                                            countText = mix.length.toString()
                                            preset = mix.firstOrNull()?.digitToInt() ?: preset
                                        }
                                        mixing = enabled
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = mixing, onCheckedChange = null)
                            Text(strings.mixDifficulties, modifier = Modifier.padding(start = MaterialTheme.spacing.small))
                        }
                    }
                    if (preset == MathChallenge.CUSTOM) {
                        HorizontalDivider()
                        Column {
                            Text(strings.mathOperations, style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(ChallengeDimensions.OPERATION_CHIP_SPACING)) {
                                listOf(
                                    '+' to strings.addition,
                                    '−' to strings.subtraction,
                                    '×' to strings.multiplication,
                                    '÷' to strings.division
                                ).forEach { (symbol, name) ->
                                    FilterChip(
                                        selected = symbol in operations,
                                        onClick = {
                                            if (symbol !in operations) operations += symbol
                                            else if (operations.length > 1) operations =
                                                operations.replace(symbol.toString(), "")
                                        },
                                        label = { Text(symbol.toString()) },
                                        modifier = Modifier.semantics { contentDescription = name },
                                    )
                                }
                            }
                        }
                        if ('+' in operations || '−' in operations) {
                            ChallengeRange(
                                strings.additionAndSubtraction,
                                MathChallenge.ADDITION_RANGES.map { "${it.first}–${it.last}" },
                                addition
                            ) { addition = it }
                        }
                        if ('×' in operations || '÷' in operations) {
                            ChallengeRange(
                                strings.multiplicationAndDivision,
                                MathChallenge.FACTOR_RANGES.map { "${it.first}–${it.last}" },
                                factors
                            ) { factors = it }
                        }
                    }
                    if (!mixing) {
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    strings.mathExample,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    sampleQuestion(draft, seed),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            TextButton(
                                onClick = { seed++ },
                                colors = actionColors
                            ) { Text(strings.refreshExample) }
                        }
                    }
                }
                SheetFooter(
                    secondaryLabel = strings.cancel.uppercase(),
                    primaryLabel = strings.applyChallenge.uppercase(),
                    onSecondaryClick = onDismiss,
                    onPrimaryClick = { if (valid) onApply(draft.normalized()) },
                    primaryEnabled = valid,
                )
            }
        }
    }
}

@Composable
private fun DifficultyChoices(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(strings.mathDifficulty, style = MaterialTheme.typography.labelLarge)
        challengePresetNames.indices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                row.forEach { level ->
                    FilterChip(
                        selected = selected == level,
                        onClick = { onSelect(level) },
                        label = {
                            Text(
                                challengePresetNames[level],
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MixedDifficultyCounts(mix: String, onMixChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        challengePresetNames.take(3).forEachIndexed { level, name ->
            val digit = level.digitToChar()
            val count = mix.count { it == digit }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                CountStepper(
                    value = count.toString(),
                    label = strings.difficultyQuestionLabel(name),
                    onValueChange = {},
                    readOnly = true,
                    canDecrease = count > 0 && mix.length > 1,
                    canIncrease = mix.length < MathChallenge.MAX_QUESTIONS,
                    onDecrease = { onMixChange(mix.replaceFirst(digit.toString(), "")) },
                    onIncrease = { onMixChange((mix + digit).toList().sorted().joinToString("")) },
                )
            }
        }
        Text(
            strings.easiestFirst,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionCountControl(countText: String, onCountChange: (String) -> Unit) {
    val count = countText.toIntOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.questions,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge
            )
            CountStepper(
                value = countText,
                label = strings.questions,
                onValueChange = { value ->
                    val number = value.toIntOrNull()
                    if (value.isEmpty() || (value.length <= 2 && number != null && number in 0..MathChallenge.MAX_QUESTIONS)) onCountChange(
                        value
                    )
                },
                readOnly = false,
                isError = count == null || count !in 1..MathChallenge.MAX_QUESTIONS,
                canDecrease = count != null && count > 1,
                canIncrease = count == null || count < MathChallenge.MAX_QUESTIONS,
                onDecrease = { onCountChange(((count ?: 1) - 1).coerceAtLeast(1).toString()) },
                onIncrease = {
                    onCountChange(
                        ((count ?: 0) + 1).coerceAtMost(MathChallenge.MAX_QUESTIONS).toString()
                    )
                },
            )
        }
        Text(
            strings.questionCountHint(MathChallenge.MAX_QUESTIONS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CountStepper(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    isError: Boolean = false,
) {
    val decreaseLabel = strings.decreaseQuestionCount(label)
    val increaseLabel = strings.increaseQuestionCount(label)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier
                .width(ChallengeDimensions.MIN_TOUCH_TARGET)
                .heightIn(min = ChallengeDimensions.MIN_TOUCH_TARGET)
                .semantics { contentDescription = decreaseLabel },
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(ChallengeDimensions.COUNT_FIELD_WIDTH)
                .semantics { contentDescription = label },
            readOnly = readOnly,
            singleLine = true,
            isError = isError,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        TextButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier
                .width(ChallengeDimensions.MIN_TOUCH_TARGET)
                .heightIn(min = ChallengeDimensions.MIN_TOUCH_TARGET)
                .semantics { contentDescription = increaseLabel },
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ChallengeRange(
    label: String,
    choices: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    open = true
                }
                .padding(vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChallengeDimensions.ROW_SPACING),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = choices[selected],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(imageVector = KeyboardArrowDown, contentDescription = strings.chooseRange)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEachIndexed { index, text ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(index); open = false })
            }
        }
    }
}

private val challengePresetNames: List<String>
    @Composable get() = strings.mathDifficultyNames

private fun sampleQuestion(challenge: MathChallenge, seed: Int): String =
    buildQuestionString(
        generateChallengeProblems(
            challenge.copy(questionCount = 1),
            Random(seed)
        ).first()
    ) + " = ?"

@Preview
@Composable
private fun MathChallengeSettingsPreview() {
    MathAlarmTheme(darkTheme = false) {
        Surface {
            MathChallengeSettings(
                challenge = MathChallenge(difficulty = 1),
                onEdit = {},
                modifier = Modifier.padding(MaterialTheme.spacing.medium)
            )
        }
    }
}

private object ChallengeDimensions {
    val ROW_SPACING = 12.dp
    val MIN_TOUCH_TARGET = 48.dp
    val MAX_CONTENT_WIDTH = 640.dp
    val MIN_CONTROLS_HEIGHT = 280.dp
    val OPERATION_CHIP_SPACING = 6.dp
    val COUNT_FIELD_WIDTH = 64.dp
}
