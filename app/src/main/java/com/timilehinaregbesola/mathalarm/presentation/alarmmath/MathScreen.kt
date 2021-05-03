package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.timilehinaregbesola.mathalarm.presentation.ui.*

@Composable
fun MathScreen(
    navController: NavHostController,
    alarmId: Long
) {
    Surface(
        Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .padding(horizontal = 24.dp),
                color = indicatorColor,
                progress = 0.7f
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "2", fontSize = 70.sp, color = Color(0xFF272727), fontWeight = FontWeight.Bold)
                Text(text = "+", fontSize = 70.sp, color = Color(0xFF272727), fontWeight = FontWeight.Bold)
                Text(text = "2", fontSize = 70.sp, color = Color(0xFF272727), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 62.dp),
                backgroundColor = unSelectedDay,
                elevation = 0.dp,
                shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "=", fontSize = 30.sp, color = Color(0xFF878787))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 62.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.height(120.dp)) {
                    Button(
                        modifier = Modifier
                            .height(55.dp)
                            .width(120.dp),
                        onClick = { /*TODO*/ },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = clearButtonColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "CLEAR", fontSize = 19.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        modifier = Modifier
                            .height(55.dp)
                            .width(120.dp),
                        onClick = { /*TODO*/ },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = snoozeButtonColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "SNOOZE", fontSize = 19.sp)
                    }
                }
                Button(
                    modifier = Modifier
                        .height(120.dp)
                        .width(120.dp),
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = enterButtonColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "ENTER", fontSize = 19.sp)
                }
            }
        }
    }
}

@Preview
@Composable
fun MathScreenPreview() {
    MathAlarmTheme {
        MathScreen(
            rememberNavController(),
            1L
        )
    }
}
