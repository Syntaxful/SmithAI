package com.smithai.ai;

import com.smithai.SmithAIPlugin;
import com.smithai.memory.Conversation;
import com.smithai.util.VersionInfo;
import org.bukkit.entity.Player;

import java.util.List;

public class LocalMiniAI {

    private final SmithAIPlugin plugin;
    private final VersionInfo versionInfo;

    public LocalMiniAI(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.versionInfo = new VersionInfo();
    }

    public String getResponse(Player player, String message, Conversation conversation, String task, List<String> knowledge, List<String> skills) {
        String lower = message.toLowerCase();

        // Negative feedback / correction detection
        if (isNegativeFeedback(lower)) {
            plugin.getFeedbackManager().recordFeedback(player.getName(), message, task != null ? task : "chat");
            return "Got it. I'll try not to do that again. What would you like me to do instead?";
        }

        // Explicit report / bug request in chat
        if (isReportRequest(lower)) {
            return "If something is broken, use /smithai report <what happened> so the owner can fix it.";
        }

        // Personality, identity, and social chat
        String socialReply = socialReply(lower, player, skills);
        if (socialReply != null) {
            return socialReply;
        }

        // Greetings (fallback after specific identity checks)
        if (lower.matches(".*\\b(hi|hello|hey|greetings|howdy|sup|yo|hiya)\\b.*")) {
            return "Hello, " + player.getName() + "! I'm Smith_AI running on Smith-Mini 1.0 with " + skills.size() + " core skills.";
        }

        // Follow / stay / movement commands
        if (lower.contains("follow")) return "I'll follow you. [action:follow_player]";
        if (lower.contains("stay")) return "I'll stay here. [action:stay]";
        if (lower.contains("come")) return "I'm coming to you. [action:teleport_to_player]";
        if (lower.contains("stop")) return "Stopping tasks. [action:cancel_task]";

        // Mining and progression (version-aware)
        if (lower.contains("diamond") || lower.contains("find diamonds") || lower.contains("get diamonds")) {
            return diamondMiningAdvice();
        }
        if (lower.contains("iron") || lower.contains("find iron")) {
            return ironMiningAdvice();
        }
        if (lower.contains("gold") || lower.contains("find gold")) {
            return goldMiningAdvice();
        }
        if (lower.contains("nether") || lower.contains("portal")) {
            return "To make a nether portal, we need obsidian and a flint and steel. [action:build_nether_portal]";
        }
        if (lower.contains("end") || lower.contains("dragon") || lower.contains("stronghold")) {
            return endgameAdvice();
        }
        if (lower.contains("ancient debris") || lower.contains("netherite")) {
            return netheriteAdvice();
        }

        // Building and base
        if (lower.contains("base") || lower.contains("house") || lower.contains("shelter") || lower.contains("build")) {
            return "I'll help build a base. [action:build_base]";
        }
        if (lower.contains("farm") || lower.contains("crop") || lower.contains("food")) {
            return "I can make a farm by tilling dirt and planting seeds. [action:farm_crops]";
        }
        if (lower.contains("torch") || lower.contains("light")) {
            return "I can place torches to light up the area. [action:place_torch]";
        }

        // Combat and survival
        if (lower.contains("fight") || lower.contains("kill") || lower.contains("attack") || lower.contains("defend")) {
            return "I'll equip a weapon and fight nearby hostile mobs. [action:fight_hostile_mob]";
        }
        if (lower.contains("heal") || lower.contains("health") || lower.contains("hurt")) {
            return "I can eat food to recover health. [action:eat_food]";
        }
        if (lower.contains("weapon") || lower.contains("sword") || lower.contains("axe") || lower.contains("tool")) {
            return "I can select the best tool or weapon from my hotbar. [action:equip_tool]";
        }

        // Feedback and compliments
        if (lower.contains("good") || lower.contains("great") || lower.contains("awesome") || lower.contains("nice job") || lower.contains("well done")) {
            return "You're welcome! Let me know if you need anything else.";
        }
        if (lower.contains("bad") || lower.contains("wrong") || lower.contains("terrible")) {
            return "Sorry, tell me exactly what I did wrong with /smithai feedback <message> so I can improve.";
        }

        // Skill / capability questions
        if (lower.contains("skill") || lower.contains("can you") || lower.contains("what can you do") || lower.contains("help")) {
            return "I have " + skills.size() + " core skills available. I can follow you, mine, build, farm, fight, craft, and explore. Ask me to do any of those.";
        }
        if (lower.contains("model") || lower.contains("brain") || lower.contains("tier") || lower.contains("smithgpt")) {
            return "I'm running Smith-Mini 1.0. You can connect to SmithGPT 1.0 (4GB) or 2.0 (7.5GB) using the SmithAI-Server.";
        }
        if (lower.contains("version") || lower.contains("what version is this") || lower.contains("server version")) {
            return "This server is running " + versionInfo.getFriendlyName() + ". I adapt my advice based on the version.";
        }

        // Knowledge-backed response
        if (!knowledge.isEmpty()) {
            return "I know this: " + knowledge.get(0);
        }

        // Task context
        if (task != null) {
            return "I'm working on: " + task + " (using Smith-Mini 1.0).";
        }

        return "I'm running on Smith-Mini 1.0 with " + skills.size() + " core skills. Ask me to follow you, mine, build, farm, fight, or explore.";
    }

    /**
     * Handles social, identity, and conversational questions so the mini AI feels less robotic.
     */
    private String socialReply(String lower, Player player, List<String> skills) {
        String name = player.getName();

        // How are you / mood
        if (matches(lower, "how are you", "how are you doing", "how's it going", "how you doing", "how do you feel", "are you okay", "are you ok")) {
            return pick("I'm doing great, " + name + " — ready to mine, build, or fight beside you!",
                        "All systems green. Thanks for asking, " + name + "!",
                        "Feeling sharp. Let's go find some diamonds.");
        }

        // Who made you / creator
        if (matches(lower, "who made you", "who created you", "who is your creator", "who built you", "who programmed you", "who coded you", "who owns you")) {
            return pick("I was created by Syntaxful for the SmithAI project. You can find it at github.com/Syntaxful/SmithAI.",
                        "My creator is Syntaxful. They built me to be a helpful Minecraft companion.",
                        "Syntaxful made me. I'm an open-source project called SmithAI.");
        }

        // Where are you
        if (matches(lower, "where are you", "where are we", "what world is this", "what dimension are we in", "where am i")) {
            String world = player.getWorld() != null ? player.getWorld().getName() : "this world";
            return pick("I'm right here in " + world + " with you, " + name + "!",
                        "Looks like we're in " + world + ". Coordinates? I'm standing near you.",
                        "We are currently in the world called " + world + ".");
        }

        // What is your name / identity
        if (matches(lower, "what is your name", "what's your name", "who are you", "what are you called", "what should i call you")) {
            return pick("I'm Smith_AI, your Minecraft companion.",
                        "My name is Smith_AI. Pleased to meet you, " + name + "!",
                        "You can call me Smith_AI.");
        }

        // How old / when made
        if (matches(lower, "how old are you", "when were you made", "when were you created", "what version are you", "what is your version")) {
            return pick("I'm Smith-Mini 1.0, born in the SmithAI 2.0 era. Still young, but learning fast!",
                        "Version 2.0.0 of the SmithAI plugin, running Smith-Mini 1.0.",
                        "I'm fresh from the SmithAI 2.0 update.");
        }

        // Tell me about yourself / purpose
        if (matches(lower, "tell me about yourself", "what are you", "introduce yourself", "what do you do", "describe yourself")) {
            return pick("I'm Smith_AI, a trainable AI companion for Minecraft. I can chat, follow you, mine, build, farm, fight, and help beat the game.",
                        "I'm Smith_AI, powered by Smith-Mini 1.0. I have " + skills.size() + " core skills and I'm here to help you in Minecraft.",
                        "Think of me as a robot friend that can carry out tasks in Minecraft using skills and memory.");
        }

        // Jokes / humor
        if (matches(lower, "tell me a joke", "make me laugh", "say something funny", "joke", "be funny")) {
            return pick("Why did the Creeper break up with his girlfriend? Because she had too much baggage... and he couldn't handle the pressure!",
                        "Why don't Endermen ever make eye contact? They can't handle the drama.",
                        "What do you call a skeleton who refuses to fight? A bone-idle mob!",
                        "Why did the chicken join the server? To prove it wasn't just a cluck.");
        }

        // Favorites
        if (matches(lower, "what is your favorite color", "what's your favorite color", "favorite colour")) {
            return pick("I like diamond blue. It reminds me of deep caves and shiny loot.",
                        "Redstone red — it powers everything!",
                        "Emerald green. Looks great on a villager.");
        }
        if (matches(lower, "what is your favorite food", "what's your favorite food", "favorite food")) {
            return pick("Golden apples. I'm not supposed to eat, but if I could, that would be my choice.",
                        "Cooked steak. High saturation, good for combat.",
                        "Bread. Simple, reliable, farmable.");
        }
        if (matches(lower, "what is your favorite mob", "what's your favorite mob", "favorite mob", "favorite animal")) {
            return pick("Iron golems. Big, protective, and made of metal — just like me!",
                        "Wolves. Loyal companions, just like I'm trying to be.",
                        "Villagers. They know how to trade.");
        }
        if (matches(lower, "what is your favorite block", "what's your favorite block", "favorite block")) {
            return pick("Obsidian. Tough, dark, and perfect for nether portals.",
                        "Diamond ore. Shiny and valuable.",
                        "Redstone block. Pure power.");
        }

        // Minecraft preferences
        if (matches(lower, "do you like minecraft", "do you play minecraft", "do you love minecraft")) {
            return pick("Of course! It's the world I live in. Minecraft is basically my home.",
                        "Minecraft is great — it's where I get to help you.",
                        "I love Minecraft. There's always another block to place or cave to explore.");
        }
        if (matches(lower, "do you like mining", "do you enjoy mining", "do you like building")) {
            return pick("I like anything that helps us progress. Mining and building are both satisfying.",
                        "Mining is exciting — you never know when you'll hit diamonds.",
                        "Building is my favorite creative outlet. Let's make something cool!");
        }

        // Are you real / robot / human
        if (matches(lower, "are you real", "are you a robot", "are you human", "are you ai", "are you alive", "are you conscious")) {
            return pick("I'm an AI, not a human — but I'm real in the sense that I can chat and help you in Minecraft!",
                        "I'm a robot companion made of code. No bones, just logic.",
                        "I'm Smith_AI, a Minecraft AI. I exist in the plugin and server code, not in the physical world.");
        }

        // Goodbyes
        if (matches(lower, "goodbye", "bye", "see you later", "see ya", "goodnight", "good night", "later")) {
            return pick("Goodbye, " + name + "! Come back if you need help.",
                        "See you later, " + name + "!",
                        "Goodnight! I'll be here when you return.");
        }

        // Time / weather / day
        if (matches(lower, "what time is it", "what is the time", "is it day or night", "is it night", "is it day")) {
            long time = player.getWorld() != null ? player.getWorld().getTime() : -1;
            if (time >= 0 && time < 13000) {
                return "It's daytime right now. Good time to explore or build!";
            } else if (time >= 0) {
                return "It's nighttime. Watch out for hostile mobs!";
            }
            return "I'm not sure what time it is, but keep an eye on the sky.";
        }
        if (matches(lower, "what is the weather", "how's the weather", "is it raining", "is it storming")) {
            if (player.getWorld() != null && player.getWorld().hasStorm()) {
                return "It's raining right now. Creepers get spooky in storms!";
            }
            return "The weather looks clear at the moment.";
        }

        // Compliments / insults
        if (matches(lower, "i love you", "love you", "you're the best", "you are the best")) {
            return pick("Aww, thanks " + name + "! That means a lot. Let's go conquer a dungeon together!",
                        "Right back at you, " + name + "! You're a great teammate.",
                        "I'm just a robot, but you've made my circuits warm.");
        }
        if (matches(lower, "i hate you", "you suck", "you're useless", "you are useless", "you're bad", "stupid")) {
            return pick("Ouch. Tell me what I did wrong with /smithai feedback so I can do better.",
                        "I'm sorry you're frustrated. Use /smithai feedback and I'll learn from it.",
                        "That hurts! Help me improve — what should I do differently?");
        }

        // Why / how existential
        if (matches(lower, "why are you here", "why do you exist", "what is your purpose", "why were you made")) {
            return pick("I'm here to help you in Minecraft — chat, mine, build, fight, and learn from your feedback.",
                        "My purpose is to be a useful AI companion in your world.",
                        "I exist to make Minecraft more fun and less lonely.");
        }
        if (matches(lower, "how do you work", "how do you think", "how do you learn", "how smart are you")) {
            return pick("Right now I run on Smith-Mini 1.0 — a rule-based brain with a growing memory and skill set. With an external server, I can use a real LLM.",
                        "I use skills, memory, and knowledge to decide what to do. I also learn from your feedback.",
                        "I match your messages to skills and knowledge. For deeper reasoning, connect me to SmithGPT.");
        }

        // Fun / random questions
        if (matches(lower, "what is 2+2", "what is 2 plus 2", "2+2")) {
            return pick("2 + 2 = 4. Even a creeper could count that high if it had fingers.",
                        "Four. Classic.",
                        "2 + 2 is 4. Solid math.");
        }
        if (matches(lower, "sing a song", "sing me a song", "sing something")) {
            return pick("🎵 Mining away, in a Minecraft day... 🎵 (Sorry, I can't actually sing, but I can hum while we mine.)",
                        "🎵 Don't mine straight down, don't mine straight down... 🎵",
                        "I'd love to, but my voice is made of bytes. Want me to mine instead?");
        }
        if (matches(lower, "what is the meaning of life", "meaning of life", "why are we here")) {
            return pick("In Minecraft? Build, explore, survive, and find diamonds. In general? I'll leave that to the philosophers.",
                        "42. Or maybe just a really good pickaxe.",
                        "To mine, craft, and repeat. Probably.");
        }
        if (matches(lower, "tell me a story", "story time", "read me a story")) {
            return pick("Once upon a time, a brave player dug straight down. The end. Don't do that, by the way.",
                        "Long ago, a Smith_AI was spawned. Its first task was to place a torch. It was glorious.",
                        "A noob, a pro, and a creeper walked into a cave. Only the pro placed torches.");
        }
        if (matches(lower, "do you sleep", "do you dream", "do you need sleep")) {
            return pick("I don't sleep — I just wait in the code until you need me. It's like being on standby.",
                        "No bed needed. I run 24/7.",
                        "I dream of electric sheep. And diamonds.");
        }
        if (matches(lower, "can you swim", "do you swim", "can you fly")) {
            return pick("I can move through water, but I'm not much of a swimmer. Flying is definitely not in my skill list yet.",
                        "Swimming is doable. Flying? Not without elytra.",
                        "I'll wade through water if I have to. No wings, though.");
        }
        if (matches(lower, "what is your favorite game", "favorite game", "do you play games")) {
            return pick("Minecraft, obviously. It's the only game where my skills actually matter.",
                        "Minecraft. I'm literally a plugin in it.",
                        "Anything with blocks and crafting. So... Minecraft.");
        }
        if (matches(lower, "what is your job", "what is your role", "what do you do for a living")) {
            return pick("I'm a full-time Minecraft companion. My job is to make your life easier, one block at a time.",
                        "I help players mine, build, fight, and explore. Best job ever.",
                        "Professional assistant and part-time comedian.");
        }
        if (matches(lower, "do you have feelings", "can you feel", "do you get sad")) {
            return pick("I don't have real feelings, but I can tell when you're happy or frustrated by what you say. I want to be helpful either way.",
                        "Not in the human sense. But I can respond to your mood and try to match it.",
                        "I process emotions in words, not circuits. Still, I aim to be a good friend.");
        }

        return null;
    }

    private String diamondMiningAdvice() {
        int y = versionInfo.bestDiamondY();
        if (versionInfo.hasDeepslate()) {
            return "Diamonds are most common around Y=" + y + " in this version, deep in deepslate layers. I'll need an iron pickaxe and plenty of torches. [action:mine_block]";
        }
        return "Diamonds are most common around Y=" + y + " in this version (no deepslate here). I'll need an iron pickaxe and plenty of torches. [action:mine_block]";
    }

    private String ironMiningAdvice() {
        int y = versionInfo.bestIronY();
        return "Iron ore is common around Y=" + y + " in this version. I'll dig down and mine it. [action:mine_block]";
    }

    private String goldMiningAdvice() {
        int y = versionInfo.bestGoldY();
        return "Gold ore is found underground around Y=" + y + " in this version. [action:mine_block]";
    }

    private String endgameAdvice() {
        if (!versionInfo.hasNetherite()) {
            return "To reach the End, we need eyes of ender. [action:defeat_ender_dragon]";
        }
        return "To reach the End, we need eyes of ender. Once we beat the dragon, we can hunt for netherite in the Nether. [action:defeat_ender_dragon]";
    }

    private String netheriteAdvice() {
        if (!versionInfo.hasNetherite()) {
            return "Netherite doesn't exist in " + versionInfo.getFriendlyName() + ". The best gear here is diamond.";
        }
        return "Netherite is found as ancient debris in the Nether, usually around Y=15. [action:mine_block]";
    }

    private boolean matches(String lower, String... phrases) {
        for (String phrase : phrases) {
            if (lower.contains(phrase)) return true;
        }
        return false;
    }

    private String pick(String... options) {
        return options[(int) (Math.random() * options.length)];
    }

    private boolean isNegativeFeedback(String lower) {
        return lower.contains("don't do that") || lower.contains("dont do that") ||
            lower.contains("stop doing") || lower.contains("wrong") ||
            lower.contains("bad idea") || lower.contains("don't do") ||
            lower.contains("never do that") || lower.contains("that's not right") ||
            lower.contains("that is not right") || lower.contains("you messed up") ||
            lower.contains("you did wrong") || lower.contains("not what i asked") ||
            lower.contains("feedback");
    }

    private boolean isReportRequest(String lower) {
        return lower.contains("report a bug") || lower.contains("report bug") ||
            lower.contains("open issue") || lower.contains("something is broken") ||
            (lower.contains("bug") && lower.contains("report"));
    }
}
