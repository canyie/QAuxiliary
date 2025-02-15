/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.ketal.dispacher

import android.view.View
import android.view.ViewGroup
import cc.ioctl.hook.msg.MultiForwardAvatarHook
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BasePersistBackgroundHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.hook.ChatItemShowQQUin
import me.ketal.hook.ShowMsgAt
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.hook.HideTroopLevel
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.isPublic

@FunctionHookEntry
object BaseBubbleBuilderHook : BasePersistBackgroundHook() {
    // Register your decorator here
    // THESE HOOKS ARE CALLED IN UI THREAD WITH A VERY HIGH FREQUENCY
    // CACHE REFLECTION METHODS AND FIELDS FOR BETTER PERFORMANCE
    // Peak frequency: ~68 invocations per second
    private val decorators = arrayOf<OnBubbleBuilder>(
        HideTroopLevel,
        ShowMsgAt,
        ChatItemShowQQUin,
        MultiForwardAvatarHook.INSTANCE
    )

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        for (m in "com.tencent.mobileqq.activity.aio.BaseBubbleBuilder".clazz?.methods!!) {
            if (m.name != "a" && (m.name != "q" && requireMinQQVersion(QQVersion.QQ_8_8_93))) continue
            if (m.returnType != View::class.java) continue
            if (!m.isPublic) continue
            if (m.parameterTypes.size != 6) continue
            m.hookAfter(this) {
                if (it.result == null) return@hookAfter
                val rootView = it.result as ViewGroup
                val msg = MsgRecordData(it.args[2])
                for (decorator in decorators) {
                    try {
                        decorator.onGetView(rootView, msg, it)
                    } catch (e: Exception) {
                        traceError(e)
                    }
                }
            }
        }
        return true
    }
}

interface OnBubbleBuilder {
    @Throws(Exception::class)
    fun onGetView(
        rootView: ViewGroup,
        chatMessage: MsgRecordData,
        param: XC_MethodHook.MethodHookParam
    )
}
