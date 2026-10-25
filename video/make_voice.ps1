param(
    [string]$OutDir = "C:\Users\siddh\Desktop\GameEngine\aetherplay-suite\video\voice",
    [string]$ScriptJson = "C:\Users\siddh\Desktop\GameEngine\aetherplay-suite\video\script.json"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Speech

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.SelectVoice("Microsoft Zira Desktop")
$synth.SetOutputToDefaultAudioDevice()  # replaced per-file below

$script = Get-Content $ScriptJson -Raw | ConvertFrom-Json

function Esc([string]$s) {
    $s -replace '&','&amp;' -replace '<','&lt;' -replace '>','&gt;'
}

foreach ($seg in $script.segments) {
    # Split into sentences and insert gentle breaks so the delivery feels soft and unhurried.
    $sentences = $seg.text -split '(?<=\.\.\.|\.|\?)\s+'
    $parts = foreach ($s in $sentences) {
        $t = Esc $s.Trim()
        if ($t) { "<s>$t</s><break time=`"420ms`"/>" }
    }
    $body = ($parts -join "`n")
    $ssml = "<speak version=`"1.0`" xml:lang=`"en-US`"><voice name=`"Microsoft Zira Desktop`"><prosody pitch=`"+38%`" rate=`"-9%`" volume=`"88`">$body</prosody></voice></speak>"
    $wav = Join-Path $OutDir ($seg.id + ".wav")
    $synth.SetOutputToWaveFile($wav)
    $synth.SpeakSsml($ssml)
    $synth.SetOutputToNull()
    Write-Output ("OK  " + $seg.id + "  ->  " + $wav)
}
Write-Output "DONE"
