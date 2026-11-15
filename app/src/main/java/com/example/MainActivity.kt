package com.example

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.controller.FruitDropController
import com.example.model.DropTarget
import com.example.model.FruitType
import com.example.service.FruitDropAccessibilityService
import com.example.service.FruitDropOverlayService
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FruitDropBlue
import com.example.ui.theme.FruitDropGreen
import com.example.ui.theme.FruitDropGreenLight
import com.example.ui.theme.FruitDropOrange
import com.example.ui.theme.FruitDropPink
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnSurfaceSecondary
import com.example.ui.theme.OnSurfaceWhite

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FruitDropController.init(this)

        setContent {
            MyApplicationTheme {
                MainAppDashboard(
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestAccessibilityPermission = { requestAccessibilityPermission() },
                    onStartOverlay = { startOverlayFlow() },
                    onStopOverlay = { stopOverlay() }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private var pendingMediaProjectionLauncher: ((Intent) -> Unit)? = null
    private var pendingNotificationLauncher: ((String) -> Unit)? = null

    fun setMediaProjectionLauncher(launcher: (Intent) -> Unit) {
        pendingMediaProjectionLauncher = launcher
    }

    fun setNotificationPermissionLauncher(launcher: (String) -> Unit) {
        pendingNotificationLauncher = launcher
    }

    fun isAccessibilityEnabled(): Boolean {
        if (FruitDropAccessibilityService.instance != null) return true
        return try {
            val expectedComponent = ComponentName(this, FruitDropAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                val componentName = ComponentName.unflattenFromString(splitter.next())
                if (componentName == expectedComponent) return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun startOverlayFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Harap aktifkan Izin Floating Window terlebih dahulu!", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }

        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "Harap aktifkan Layanan Aksesibilitas FruitDrop!", Toast.LENGTH_LONG).show()
            requestAccessibilityPermission()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationLauncher?.invoke(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        launchMediaProjectionPrompt()
    }

    fun launchMediaProjectionPrompt() {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = projectionManager.createScreenCaptureIntent()
            pendingMediaProjectionLauncher?.invoke(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal meminta izin rekam layar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun startOverlayWithCaptureData(resultCode: Int, data: Intent) {
        try {
            val intent = Intent(this, FruitDropOverlayService::class.java).apply {
                putExtra(FruitDropOverlayService.EXTRA_RESULT_CODE, resultCode)
                putExtra(FruitDropOverlayService.EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "FruitDrop Overlay Aktif! Buka Yeetalk sekarang.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menjalankan overlay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopOverlay() {
        val intent = Intent(this, FruitDropOverlayService::class.java)
        stopService(intent)
        Toast.makeText(this, "Layanan FruitDrop dimatikan.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppDashboard(
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlayPermission by remember { mutableStateOf(false) }
    val isAccessibilityConnected by FruitDropController.isAccessibilityConnected.collectAsState()
    val isOverlayRunning by FruitDropController.isOverlayRunning.collectAsState()
    val config by FruitDropController.config.collectAsState()

    // Refresh permissions on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
                val a11yEnabled = (context as? MainActivity)?.isAccessibilityEnabled()
                    ?: (FruitDropAccessibilityService.instance != null)
                FruitDropController.setAccessibilityConnected(a11yEnabled)
                FruitDropController.setOverlayRunning(FruitDropOverlayService.instance != null)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            (context as? MainActivity)?.startOverlayWithCaptureData(result.resultCode, result.data!!)
        } else {
            Toast.makeText(context, "Izin perekaman layar dibatalkan.", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        (context as? MainActivity)?.launchMediaProjectionPrompt()
    }

    DisposableEffect(Unit) {
        (context as? MainActivity)?.setMediaProjectionLauncher { intent ->
            mediaProjectionLauncher.launch(intent)
        }
        (context as? MainActivity)?.setNotificationPermissionLauncher { perm ->
            notificationPermissionLauncher.launch(perm)
        }
        onDispose { }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Aturan Buah", "Kalibrasi", "Panduan")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.fruitdrop_icon_1794722622121),
                            contentDescription = "FruitDrop App Icon",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = OnSurfaceWhite
                            )
                            Text(
                                text = "Auto-Drop Fruit Merge",
                                fontSize = 11.sp,
                                color = FruitDropGreenLight
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = FruitDropGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FruitDropGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) FruitDropGreenLight else OnSurfaceSecondary
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> DashboardTab(
                    hasOverlayPermission = hasOverlayPermission,
                    isAccessibilityConnected = isAccessibilityConnected,
                    isOverlayRunning = isOverlayRunning,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                    onStartOverlay = onStartOverlay,
                    onStopOverlay = onStopOverlay
                )
                1 -> FruitRulesTab()
                2 -> CalibrationTab()
                3 -> GuideTab()
            }
        }
    }
}

@Composable
fun DashboardTab(
    hasOverlayPermission: Boolean,
    isAccessibilityConnected: Boolean,
    isOverlayRunning: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit
) {
    val config by FruitDropController.config.collectAsState()
    val allPermissionsGranted = hasOverlayPermission && isAccessibilityConnected

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Launch Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_launch_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverlayRunning) Color(0xFF133827) else DarkSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOverlayRunning) FruitDropGreen else Color(0xFF334155)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isOverlayRunning) FruitDropGreen else Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverlayRunning) Icons.Default.PlayArrow else Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isOverlayRunning) "Overlay Aktif & Siap!" else "Auto-Drop Bot Siap",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceWhite
                    )

                    Text(
                        text = if (isOverlayRunning)
                            "Panel FruitDrop mengapung di layar. Buka Yeetalk sekarang!"
                        else
                            "Deteksi buah real-time: Kiwi & Peach ke KIRI, buah kecil ke KANAN.",
                        fontSize = 12.sp,
                        color = OnSurfaceSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (!isOverlayRunning) {
                        Button(
                            onClick = onStartOverlay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_start_overlay"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FruitDropGreen
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mulai Floating Overlay",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }
                    } else {
                        Button(
                            onClick = onStopOverlay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_stop_overlay"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935)
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hentikan Floating Overlay",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Live Stats Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Drop Kiri", fontSize = 12.sp, color = FruitDropGreenLight)
                        Text(
                            text = "${config.leftCount}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceWhite
                        )
                        Text(text = "Kiwi & Peach", fontSize = 10.sp, color = OnSurfaceSecondary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Drop Kanan", fontSize = 12.sp, color = FruitDropOrange)
                        Text(
                            text = "${config.rightCount}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceWhite
                        )
                        Text(text = "Buah Kecil Lain", fontSize = 10.sp, color = OnSurfaceSecondary)
                    }
                }
            }
        }

        // Checklist Izin
        item {
            Text(
                text = "Pemeriksaan Izin Sistem",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = OnSurfaceWhite
            )
        }

        item {
            PermissionCard(
                title = "1. Izin Floating Window (Overlay)",
                description = "Menampilkan panel kontrol dan penanda titik di atas layar game Yeetalk.",
                isGranted = hasOverlayPermission,
                onGrantClick = onRequestOverlayPermission,
                testTag = "perm_overlay"
            )
        }

        item {
            PermissionCard(
                title = "2. Layanan Aksesibilitas (Auto-Click)",
                description = "Melakukan klik super cepat untuk menjatuhkan buah ke kolom kiri atau kanan.",
                isGranted = isAccessibilityConnected,
                onGrantClick = onRequestAccessibilityPermission,
                testTag = "perm_accessibility"
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) Color(0xFF1E3A2E) else Color(0xFF3E2D2D)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) FruitDropGreen else Color(0xFFFF9800),
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSurfaceWhite
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = OnSurfaceSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (!isGranted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onGrantClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FruitDropBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Aktifkan Izin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Sudah Aktif ✓",
                        color = FruitDropGreenLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FruitRulesTab() {
    val config by FruitDropController.config.collectAsState()
    val fruits = listOf(
        FruitType.KIWI_HIJAU,
        FruitType.PEACH,
        FruitType.BLUEBERRY,
        FruitType.GRAPE,
        FruitType.STRAWBERRY,
        FruitType.ORANGE
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = FruitDropBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sesuai permintaan Anda: Kiwi Hijau dan Buah Persik (Peach) ditandai untuk jatuh ke PALING KIRI, sedangkan buah lain yang lebih kecil dari kiwi jatuh ke PALING KANAN.",
                        fontSize = 12.sp,
                        color = OnSurfaceWhite
                    )
                }
            }
        }

        items(fruits.size) { index ->
            val fruit = fruits[index]
            val currentTarget = config.getTargetForFruit(fruit)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rule_card_${fruit.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(fruit.composeColor)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = fruit.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = OnSurfaceWhite
                            )
                            if (fruit.isMarkedByUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ditandai ★",
                                    fontSize = 9.sp,
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFF3E2723), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = fruit.description,
                            fontSize = 11.sp,
                            color = OnSurfaceSecondary
                        )
                    }

                    // Direction Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (currentTarget == DropTarget.LEFT) FruitDropGreen else Color.Transparent)
                                .clickable { FruitDropController.setFruitRule(fruit.id, DropTarget.LEFT) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "KIRI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTarget == DropTarget.LEFT) Color.Black else Color.Gray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (currentTarget == DropTarget.RIGHT) FruitDropOrange else Color.Transparent)
                                .clickable { FruitDropController.setFruitRule(fruit.id, DropTarget.RIGHT) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "KANAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTarget == DropTarget.RIGHT) Color.Black else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalibrationTab() {
    val config by FruitDropController.config.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Speed & Delay Slider
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = FruitDropGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kecepatan & Delay Drop",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnSurfaceWhite
                        )
                    }

                    Text(
                        text = "Interval waktu antara tap: ${config.dropDelayMs} ms",
                        fontSize = 12.sp,
                        color = FruitDropGreenLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Slider(
                        value = config.dropDelayMs.toFloat(),
                        onValueChange = { FruitDropController.setDropDelay(it.toLong()) },
                        valueRange = 100f..800f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = FruitDropGreen,
                            activeTrackColor = FruitDropGreen
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("100 ms (Sangat Cepat)", fontSize = 10.sp, color = OnSurfaceSecondary)
                        Text("800 ms (Santai)", fontSize = 10.sp, color = OnSurfaceSecondary)
                    }
                }
            }
        }

        // Coordinates Calibration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = FruitDropBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Posisi Kolom & Titik Scan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnSurfaceWhite
                        )
                    }

                    Text(
                        text = "Anda juga dapat menggeser langsung lingkaran KIRI, KANAN, dan SCAN BUAH di layar!",
                        fontSize = 11.sp,
                        color = OnSurfaceSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    CoordinateSlider(
                        label = "Titik Drop Kiri (X): ${(config.dropLeftRelX * 100).toInt()}%",
                        value = config.dropLeftRelX,
                        range = 0.05f..0.45f,
                        onValueChange = { FruitDropController.updateCoordinates(dropLeftRelX = it) }
                    )

                    CoordinateSlider(
                        label = "Titik Drop Kanan (X): ${(config.dropRightRelX * 100).toInt()}%",
                        value = config.dropRightRelX,
                        range = 0.55f..0.98f,
                        onValueChange = { FruitDropController.updateCoordinates(dropRightRelX = it) }
                    )

                    CoordinateSlider(
                        label = "Ketinggian Drop (Y): ${(config.dropRelY * 100).toInt()}%",
                        value = config.dropRelY,
                        range = 0.20f..0.60f,
                        onValueChange = { FruitDropController.updateCoordinates(dropRelY = it) }
                    )

                    CoordinateSlider(
                        label = "Posisi Scan Buah (X): ${(config.scanRelX * 100).toInt()}%",
                        value = config.scanRelX,
                        range = 0.02f..0.95f,
                        onValueChange = { FruitDropController.updateCoordinates(scanRelX = it) }
                    )

                    CoordinateSlider(
                        label = "Posisi Scan Buah (Y): ${(config.scanRelY * 100).toInt()}%",
                        value = config.scanRelY,
                        range = 0.10f..0.55f,
                        onValueChange = { FruitDropController.updateCoordinates(scanRelY = it) }
                    )

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Ukuran Area Scan: ${config.scanRadiusDp} dp",
                            fontSize = 11.sp,
                            color = OnSurfaceWhite
                        )
                        Slider(
                            value = config.scanRadiusDp.toFloat(),
                            onValueChange = { FruitDropController.setScanRadiusDp(it.toInt()) },
                            valueRange = 8f..28f,
                            colors = SliderDefaults.colors(
                                thumbColor = FruitDropBlue,
                                activeTrackColor = FruitDropBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            FruitDropController.updateCoordinates(
                                scanRelX = 0.086f,
                                scanRelY = 0.281f,
                                dropLeftRelX = 0.095f,
                                dropRightRelX = 0.915f,
                                dropRelY = 0.406f
                            )
                            FruitDropController.setScanRadiusDp(13)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset ke Koordinat Bawaan Yeetalk")
                    }
                }
            }
        }
    }
}

@Composable
fun CoordinateSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 11.sp, color = OnSurfaceWhite)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = FruitDropBlue,
                activeTrackColor = FruitDropBlue
            )
        )
    }
}

@Composable
fun GuideTab() {
    val steps = listOf(
        "1. Aktifkan Izin" to "Tekan tombol 'Aktifkan Izin' untuk Floating Window dan Layanan Aksesibilitas.",
        "2. Mulai Overlay" to "Tekan 'Mulai Floating Overlay' di halaman Dashboard. Izinkan perekaman layar saat diminta.",
        "3. Buka Yeetalk" to "Buka game Fruit Merge di Yeetalk. Panel 'FruitDrop' akan mengapung di atas game Anda.",
        "4. Cek Penanda Titik" to "Pastikan lingkaran 'KIRI' berada di kolom paling kiri, 'KANAN' di kolom paling kanan, dan 'SCAN BUAH' tepat di atas lingkaran buah selanjutnya.",
        "5. Nyalakan Auto & Drop Buah 1 Manual" to "Tekan '● ON • AUTO AKTIF'. Dalam mode ANTREAN (NEXT), bot akan menunggu Anda menurunkan buah pertama secara manual lalu otomatis meneruskan buah ke-2, ke-3, dan seterusnya sesuai urutan! (Atau tekan 'Sudah Drop Buah 1 ▶' bila dua buah kembar muncul berurutan).",
        "6. Nikmati Permainan!" to "Bot akan mendeteksi buah selanjutnya dan menjatuhkannya ke kolom Kiri/Kanan secara presisi."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Panduan Penggunaan Yeetalk FruitDrop",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OnSurfaceWhite
            )
        }

        items(steps.size) { index ->
            val (stepTitle, stepDesc) = steps[index]
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(FruitDropGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stepTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OnSurfaceWhite
                        )
                        Text(
                            text = stepDesc,
                            fontSize = 11.sp,
                            color = OnSurfaceSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
