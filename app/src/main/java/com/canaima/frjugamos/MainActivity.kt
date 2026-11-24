package com.canaima.frjugamos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val Ink = Color(0xFF000000)
private val SecondaryText = Color(0xFF777777)
private val Hairline = Color(0xFFEBEBEB)
private val Canvas = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JugamosTheme {
                ScoreTrackerApp()
            }
        }
    }
}

internal data class Player(
    val id: Int,
    val name: String,
    val score: Int = 0,
)

internal enum class AppScreen {
    HOME,
    SETUP,
    GAME,
    RESULTS,
}

enum class VictoryMode {
    MOST_POINTS,
    FEWEST_POINTS,
}

internal data class TrackerState(
    val players: List<Player> = emptyList(),
    val screen: AppScreen = AppScreen.HOME,
    val victoryMode: VictoryMode = VictoryMode.MOST_POINTS,
)

class ScoreTrackerViewModel : ViewModel() {
    private val _state = MutableStateFlow(TrackerState())
    internal val state = _state.asStateFlow()
    private var nextPlayerId = 0

    fun addPlayer(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        _state.update { current ->
            current.copy(players = current.players + Player(id = nextPlayerId++, name = trimmedName))
        }
    }

    fun removePlayer(id: Int) {
        _state.update { current ->
            current.copy(players = current.players.filterNot { it.id == id })
        }
    }

    fun removeAllPlayers() {
        _state.update { current -> current.copy(players = emptyList()) }
    }

    fun setVictoryMode(mode: VictoryMode) {
        _state.update { current -> current.copy(victoryMode = mode) }
    }

    fun openSetup() {
        _state.update { current -> current.copy(screen = AppScreen.SETUP) }
    }

    fun returnHome() {
        _state.update { current -> current.copy(screen = AppScreen.HOME) }
    }

    fun startGame() {
        _state.update { current ->
            if (current.players.size >= 2) {
                current.copy(players = current.players.map { it.copy(score = 0) }, screen = AppScreen.GAME)
            } else {
                current
            }
        }
    }

    fun changeScore(id: Int, delta: Int) {
        _state.update { current ->
            current.copy(
                players = current.players.map { player ->
                    if (player.id == id) {
                        val updatedScore = player.score.toLong() + delta
                        if (updatedScore in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                            player.copy(score = updatedScore.toInt())
                        } else {
                            player
                        }
                    } else {
                        player
                    }
                },
            )
        }
    }

    fun finishGame() {
        _state.update { it.copy(screen = AppScreen.RESULTS) }
    }

    fun replay() {
        _state.update { current ->
            current.copy(
                players = current.players.map { it.copy(score = 0) },
                screen = AppScreen.GAME,
            )
        }
    }

    fun newGame() {
        _state.update { current ->
            current.copy(
                players = current.players.map { it.copy(score = 0) },
                screen = AppScreen.HOME,
                victoryMode = VictoryMode.MOST_POINTS,
            )
        }
    }
}

internal fun parseScoreAdjustment(input: String): Int? = input.trim().toIntOrNull()

internal fun rankPlayers(players: List<Player>, victoryMode: VictoryMode): List<Player> =
    when (victoryMode) {
        VictoryMode.MOST_POINTS -> players.sortedByDescending { it.score }
        VictoryMode.FEWEST_POINTS -> players.sortedBy { it.score }
    }

internal fun winningPlayers(players: List<Player>, victoryMode: VictoryMode): List<Player> {
    val rankedPlayers = rankPlayers(players, victoryMode)
    val winningScore = rankedPlayers.firstOrNull()?.score ?: return emptyList()
    return rankedPlayers.takeWhile { it.score == winningScore }
}

@Composable
private fun JugamosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            onPrimary = Color.White,
            background = Canvas,
            surface = Canvas,
            onSurface = Ink,
        ),
        content = content,
    )
}

@Composable
private fun ScoreTrackerApp(viewModel: ScoreTrackerViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.screen == AppScreen.SETUP) {
        viewModel.returnHome()
    }

    when (state.screen) {
        AppScreen.HOME -> HomeScreen(onStart = viewModel::openSetup)

        AppScreen.SETUP -> SetupScreen(
            players = state.players,
            onAddPlayer = viewModel::addPlayer,
            onRemovePlayer = viewModel::removePlayer,
            onRemoveAllPlayers = viewModel::removeAllPlayers,
            victoryMode = state.victoryMode,
            onVictoryModeChange = viewModel::setVictoryMode,
            onStart = viewModel::startGame,
        )

        AppScreen.GAME -> GameScreen(
            players = state.players,
            onChangeScore = viewModel::changeScore,
            onFinish = viewModel::finishGame,
        )

        AppScreen.RESULTS -> ResultsScreen(
            players = rankPlayers(state.players, state.victoryMode),
            victoryMode = state.victoryMode,
            onReplay = viewModel::replay,
            onNewGame = viewModel::newGame,
        )
    }
}

@Composable
private fun HomeScreen(onStart: () -> Unit) {
    var showHowItWorks by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Canvas)
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Créer une partie", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            val wideLayout = maxWidth >= 700.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wideLayout) 56.dp else 24.dp),
            ) {
                Spacer(Modifier.height(if (wideLayout) 32.dp else 18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1100.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Ink, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("j", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("jugamos", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "LE COMPTEUR DE PARTIE",
                        color = SecondaryText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.3.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                Spacer(Modifier.height(if (wideLayout) 52.dp else 38.dp))

                if (wideLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1100.dp)
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HomeIntro(
                            wideLayout = true,
                            modifier = Modifier.weight(1f),
                        )
                        ScorePreview(modifier = Modifier.weight(0.9f))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .align(Alignment.CenterHorizontally),
                    ) {
                        HomeIntro(wideLayout = false)
                        Spacer(Modifier.height(32.dp))
                        ScorePreview()
                    }
                }

                Spacer(Modifier.height(if (wideLayout) 32.dp else 24.dp))
                HowItWorks(
                    expanded = showHowItWorks,
                    onToggle = { showHowItWorks = !showHowItWorks },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1100.dp)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Pas de compte. Pas de calculs. Juste le jeu.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1100.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 24.dp),
                    color = SecondaryText,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun HomeIntro(wideLayout: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Eyebrow("LES PARTIES, EN TOUTE SIMPLICITÉ")
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Gardez le score.\nPas les comptes.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (wideLayout) 54.sp else 44.sp,
                lineHeight = if (wideLayout) 58.sp else 48.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-2.2).sp,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Ajoutez les joueurs, profitez de la partie, et laissez Jugamos s’occuper des points.",
            modifier = Modifier.widthIn(max = 430.dp),
            color = SecondaryText,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
        )
    }
}

@Composable
private fun ScorePreview(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF7F7F7),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow("PARTIE EN COURS")
                    Spacer(Modifier.height(4.dp))
                    Text("Les scores", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("01 / 04", color = SecondaryText, style = tabularNumberStyle(12.sp))
            }
            PreviewPlayer(name = "Camille", score = "24", isLeading = true)
            Spacer(Modifier.height(10.dp))
            PreviewPlayer(name = "Alex", score = "18", isLeading = false)
            Spacer(Modifier.height(10.dp))
            PreviewPlayer(name = "Sam", score = "12", isLeading = false)
        }
    }
}

@Composable
private fun PreviewPlayer(name: String, score: String, isLeading: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Hairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isLeading) {
                    Text(
                        "EN TÊTE",
                        color = SecondaryText,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                text = score,
                style = tabularNumberStyle(25.sp, FontWeight.SemiBold),
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFFF4F4F4), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun HowItWorks(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .animateContentSize()
            .clickable(onClick = onToggle)
            .semantics { contentDescription = "Comment ça marche" },
        color = Color(0xFFF7F7F7),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Comment ça marche",
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (expanded) "−" else "+",
                    color = SecondaryText,
                    fontSize = 20.sp,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                HairlineDivider()
                Spacer(Modifier.height(12.dp))
                Text("01   Ajoutez les joueurs autour de la table", fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Text("02   Notez les points au fil de la partie", fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Text("03   Découvrez le classement final", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SetupScreen(
    players: List<Player>,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (Int) -> Unit,
    onRemoveAllPlayers: () -> Unit,
    victoryMode: VictoryMode,
    onVictoryModeChange: (VictoryMode) -> Unit,
    onStart: () -> Unit,
) {
    var playerName by remember { mutableStateOf("") }
    var showVictorySettings by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    if (showVictorySettings) {
        VictoryModeDialog(
            selectedMode = victoryMode,
            onSelectMode = {
                onVictoryModeChange(it)
                showVictorySettings = false
            },
            onDismiss = { showVictorySettings = false },
        )
    }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            BottomAction(
                label = "Commencer",
                enabled = players.size >= 2,
                onClick = onStart,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Eyebrow("JUGAMOS  /  NOUVELLE PARTIE")
                IconButton(
                    onClick = { showVictorySettings = true },
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Paramètres de victoire" },
                ) {
                    Text(
                        text = "⚙",
                        color = Ink,
                        fontSize = 21.sp,
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = "Qui joue ?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.4).sp,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (players.isEmpty()) "Ajoutez au moins deux joueurs pour commencer."
                    else "${players.size} ${if (players.size == 1) "joueur" else "joueurs"} prêts.",
                    modifier = Modifier.weight(1f),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (players.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRemoveAllPlayers,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Hairline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryText),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp,
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Supprimer tous les joueurs"
                        },
                    ) {
                        Text("Tout effacer", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 18.sp),
                    cursorBrush = SolidColor(Ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onAddPlayer(playerName)
                            playerName = ""
                            keyboard?.hide()
                        },
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (playerName.isEmpty()) {
                                Text("Nom du joueur", color = SecondaryText, fontSize = 18.sp)
                            }
                            innerTextField()
                        }
                    },
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Ajouter",
                    modifier = Modifier
                        .clickable {
                            onAddPlayer(playerName)
                            playerName = ""
                            keyboard?.hide()
                        }
                        .padding(vertical = 8.dp),
                    color = Ink,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
            }
            HairlineDivider()
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(players, key = { _, player -> player.id }) { index, player ->
                    PlayerSetupRow(
                        number = index + 1,
                        player = player,
                        onRemove = { onRemovePlayer(player.id) },
                    )
                    HairlineDivider()
                }
            }
        }
    }
}

@Composable
private fun VictoryModeDialog(
    selectedMode: VictoryMode,
    onSelectMode: (VictoryMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Canvas,
        title = { Text("Comment gagner ?", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VictoryMode.values().forEach { mode ->
                    val label = when (mode) {
                        VictoryMode.MOST_POINTS -> "Plus de points"
                        VictoryMode.FEWEST_POINTS -> "Moins de points"
                    }
                    OutlinedButton(
                        onClick = { onSelectMode(mode) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (mode == selectedMode) Ink else Hairline,
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (mode == selectedMode) Ink else SecondaryText,
                        ),
                    ) {
                        Text(label, fontWeight = if (mode == selectedMode) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = Ink)
            }
        },
    )
}

@Composable
private fun PlayerSetupRow(number: Int, player: Player, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            color = SecondaryText,
            style = tabularNumberStyle(13.sp),
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = player.name,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "×",
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onRemove)
                .semantics { contentDescription = "Supprimer ${player.name}" }
                .padding(8.dp),
            color = SecondaryText,
            fontSize = 23.sp,
            lineHeight = 24.sp,
        )
    }
}

@Composable
private fun GameScreen(
    players: List<Player>,
    onChangeScore: (Int, Int) -> Unit,
    onFinish: () -> Unit,
) {
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            BottomAction(
                label = "Terminer",
                enabled = true,
                onClick = onFinish,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Eyebrow("JUGAMOS  /  EN COURS")
            Spacer(Modifier.height(26.dp))
            Text(
                text = "Les scores",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.4).sp,
                ),
            )
            Spacer(Modifier.height(24.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(players, key = { _, player -> player.id }) { index, player ->
                    ScoreCard(
                        player = player,
                        index = index + 1,
                        onDecrement = { onChangeScore(player.id, -1) },
                        onIncrement = { onChangeScore(player.id, 1) },
                        onScoreClick = { selectedPlayer = player },
                    )
                }
            }
        }
    }

    selectedPlayer?.let { player ->
        ScoreAdjustmentDialog(
            player = player,
            onDismiss = { selectedPlayer = null },
            onApply = { delta ->
                onChangeScore(player.id, delta)
                selectedPlayer = null
            },
        )
    }
}

@Composable
private fun ScoreCard(
    player: Player,
    index: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onScoreClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Canvas,
        border = BorderStroke(1.dp, Hairline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow(index.toString().padStart(2, '0'))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = player.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ScoreButton(label = "−", description = "Retirer un point à ${player.name}", onClick = onDecrement)
            Text(
                text = player.score.toString(),
                modifier = Modifier
                    .width(64.dp)
                    .clickable(onClick = onScoreClick)
                    .semantics { contentDescription = "Modifier le score de ${player.name}" },
                style = tabularNumberStyle(34.sp, FontWeight.Medium),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            ScoreButton(label = "+", description = "Ajouter un point à ${player.name}", onClick = onIncrement)
        }
    }
}

@Composable
private fun ScoreAdjustmentDialog(
    player: Player,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    var input by remember(player.id) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val adjustment = parseScoreAdjustment(input)
    val updatedScore = adjustment?.let { player.score.toLong() + it }
    val canApply = updatedScore != null &&
        updatedScore in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()

    LaunchedEffect(player.id) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun applyAdjustment() {
        adjustment?.takeIf { canApply }?.let(onApply)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Canvas,
        title = {
            Text("Modifier le score", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                Text(
                    text = "${player.name} · score actuel ${player.score}",
                    color = SecondaryText,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = tabularNumberStyle(24.sp, FontWeight.Medium),
                        cursorBrush = SolidColor(Ink),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { applyAdjustment() }),
                        decorationBox = { innerTextField ->
                            Column {
                                Box {
                                    if (input.isEmpty()) {
                                        Text(
                                            text = "Ex. +5 ou −3",
                                            color = SecondaryText,
                                            fontSize = 20.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                                Spacer(Modifier.height(12.dp))
                                HairlineDivider()
                            }
                        },
                    )
                    Text(
                        text = "±",
                        modifier = Modifier
                            .clickable {
                                input = when {
                                    input.startsWith("-") -> input.drop(1)
                                    input.startsWith("+") -> "-${input.drop(1)}"
                                    else -> "-$input"
                                }
                            }
                            .semantics { contentDescription = "Inverser le signe du montant" }
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Utilisez ± pour basculer entre ajout et soustraction.",
                    color = SecondaryText,
                    fontSize = 13.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::applyAdjustment, enabled = canApply) {
                Text("Appliquer", color = if (canApply) Ink else SecondaryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = SecondaryText)
            }
        },
    )
}

@Composable
private fun ScoreButton(label: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = Color(0xFFF7F7F7),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun ResultsScreen(
    players: List<Player>,
    victoryMode: VictoryMode,
    onReplay: () -> Unit,
    onNewGame: () -> Unit,
) {
    val tiedWinners = winningPlayers(players, victoryMode)
    val winner = tiedWinners.firstOrNull()
    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Canvas)
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onNewGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Canvas,
                        contentColor = Ink,
                    ),
                    border = BorderStroke(1.dp, Hairline),
                ) {
                    Text("Nouvelle partie", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onReplay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Rejouer", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Eyebrow("JUGAMOS  /  RÉSULTATS")
            Spacer(Modifier.height(30.dp))
            if (winner != null) {
                Text(
                    text = if (tiedWinners.size == 1) {
                        "LA VICTOIRE REVIENT À"
                    } else {
                        "ÉGALITÉ EN TÊTE"
                    },
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = tiedWinners.joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.6).sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = winner.score.toString(),
                        style = tabularNumberStyle(64.sp, FontWeight.Medium),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (victoryMode == VictoryMode.MOST_POINTS) {
                            if (winner.score == 1) "POINT" else "POINTS"
                        } else {
                            if (winner.score == 1) "POINT (LE MOINS)" else "POINTS (LE MOINS)"
                        },
                        modifier = Modifier.padding(bottom = 10.dp),
                        color = SecondaryText,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            HairlineDivider()
            Spacer(Modifier.height(20.dp))
            Text("Classement", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                itemsIndexed(players, key = { _, player -> player.id }) { index, player ->
                    RankingRow(rank = index + 1, player = player)
                    HairlineDivider()
                }
            }
        }
    }
}

@Composable
private fun RankingRow(rank: Int, player: Player) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString().padStart(2, '0'),
            modifier = Modifier.width(42.dp),
            color = SecondaryText,
            style = tabularNumberStyle(14.sp),
        )
        Text(
            text = player.name,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = player.score.toString(),
            style = tabularNumberStyle(18.sp, FontWeight.Medium),
        )
    }
}

@Composable
private fun BottomAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Canvas)
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 16.dp),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Ink,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE5E5E5),
                disabledContentColor = Color(0xFF999999),
            ),
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text = text,
        color = SecondaryText,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
        ),
    )
}

@Composable
private fun HairlineDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline),
    )
}

private fun tabularNumberStyle(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        color = Ink,
        fontSize = size,
        fontWeight = weight,
        fontFeatureSettings = "tnum",
    )
