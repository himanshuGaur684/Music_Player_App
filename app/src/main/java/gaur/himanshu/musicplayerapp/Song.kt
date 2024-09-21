package gaur.himanshu.musicplayerapp

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

data class Track(
    val name: String,
    val desc: String,
    @RawRes val id: Int,
    @DrawableRes val image:Int
){
    constructor():this("","",R.raw.one,R.drawable.music)
}

val songs = listOf(
    Track(
        name = "First song",
        desc = "First song description",
        R.raw.one,
        R.drawable.one,
    ),Track(
        name = "Second song",
        desc = "Second song description",
        R.raw.two,
        R.drawable.two,
    ),Track(
        name = "Third song",
        desc = "Third song description",
        R.raw.three,
        R.drawable.three,
    ),Track(
        name = "Four song",
        desc = "Four song description",
        R.raw.four,
        R.drawable.four,
    ),Track(
        name = "Five song",
        desc = "Five song description",
        R.raw.five,
        R.drawable.five,
    ),
)
