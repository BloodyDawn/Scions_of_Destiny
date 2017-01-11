#Hunt of the Golden Ram Mercenary Force
# Made by Polo - Have fun!..... fix & addition by t0rm3nt0r and LEX (adapted for L2Jserver by roko91)

import sys
from net.sf.l2j import Config
from net.sf.l2j.gameserver.datatables import SkillTable
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest

qn = "628_HuntGoldenRam"

#Npcs
KAHMAN = 8554
ABERCROMBIE = 8555
SELINA = 8556

#Items
CHITIN = 7248   #Splinter Stakato Chitin
CHITIN2 = 7249  #Needle Stakato Chitin
RECRUIT = 7246  #Golden Ram Badge - Recruit
SOLDIER = 7247  #Golden Ram Badge - Soldier
GOLDEN_RAM_COIN = 7251 #Golden Ram Coin

#chances
MAX=100
CHANCE={
    1508:50,
    1509:43,
    1510:52,
    1511:57,
    1512:75,
    1513:50,
    1514:43,
    1515:52,
    1516:53,
    1517:74
}

BUFF={
"1":[4404,2,2],#Focus: Requires 2 Golden Ram Chits
"2":[4405,2,2],#Death Whisper: Requires 2 Golden Ram Chits
"3":[4393,3,3],#Might: Requires 3 Golden Ram Chits
"4":[4400,2,3],#Acumen: Requires 3 Golden Ram Chits
"5":[4397,1,3],#Berserker: Requires 3 Golden Ram Chits
"6":[4399,2,3],#Vampiric Rage: Requires 3 Golden Ram Chits
"7":[4401,1,6],#Empower: Requires 6 Golden Ram Chits
"8":[4402,2,6],#Haste: Requires 6 Golden Ram Chits
}

#needed count
count = 100

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = range(7246,7250)

 def onAdvEvent (self,event,npc,player) :
    st = player.getQuestState(qn)
    if not st: return
    htmltext = event
    cond = st.getInt("cond")
    if htmltext == "8554-03a.htm" : #Giving 100 Splinter Stakato Chitins. Getting Recruit mark
       if st.getQuestItemsCount(CHITIN)>=count and cond == 1 :
          st.set("cond","2")
          st.takeItems(CHITIN,-1)
          st.giveItems(RECRUIT,1)
          htmltext = "8554-04.htm"
    elif event == "8554-07.htm" : #Cancelling the quest
       st.exitQuest(1)
       st.playSound("ItemSound.quest_giveup")
    elif event in BUFF.keys() and cond == 3 : #Asking for buff
        skillId,level,coins=BUFF[event]
        if st.getQuestItemsCount(GOLDEN_RAM_COIN) >= coins :
          st.takeItems(GOLDEN_RAM_COIN,coins)
          npc.setTarget(player)
          npc.doCast(SkillTable.getInstance().getInfo(skillId,level))
          htmltext = "8556-1.htm"
        else :
          htmltext = "You don't have required items."
    return htmltext

 def onTalk (self,npc,st):
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   if st :
       npcId = npc.getNpcId()
       cond = st.getInt("cond")
       id = st.getState()
       if id == COMPLETED :
         st.setState(STARTED)
         st.set("cond","3")
       chitin1=st.getQuestItemsCount(CHITIN)
       chitin2=st.getQuestItemsCount(CHITIN2)
       if cond == 0 : #Starting the quest
          if st.getPlayer().getLevel()>= 66 : #Succesful start: Kill Splinter Stakato
             htmltext = "8554-02.htm"
             st.set("cond","1")
             st.setState(STARTED)
             st.playSound("ItemSound.quest_accept")
          else :
             htmltext = "8554-01.htm" #not qualified
             st.exitQuest(1)
       elif id == STARTED :
           if cond == 1 : #Bringin Splinter Stakato chitins
              if npcId == KAHMAN :
                 if chitin1>=count :
                    htmltext = "8554-03.htm" #Enough. Ready to give
                 else:
                    htmltext = "8554-03a.htm" # Need more chitins
           elif cond == 2 : # Bring more chitins of Splinter and Needle Stakato
              if npcId == ABERCROMBIE : # Trader: first multisell
                 htmltext = "8555-1.htm" 
                 return htmltext
              if npcId == SELINA : # Buffer: not qualified
                 return htmltext
              elif chitin1>=count and chitin2>=count : #Enough chitins. Ending the quest, Soldier mark.
                 htmltext = "8554-05.htm"
                 st.takeItems(CHITIN,-1)
                 st.takeItems(CHITIN2,-1)
                 st.takeItems(RECRUIT,1)
                 st.giveItems(SOLDIER,1)
                 st.set("cond","3")
                 st.playSound("ItemSound.quest_finish")
              elif not chitin1 and not chitin2: #Have no chitins. Can stop the quest
                 htmltext = "8554-04b.htm"
              else :
                 htmltext = "8554-04a.htm" #Not enough chitins. Bring more
           elif cond == 3 : #Soldier mark - last stage
              htmltext = "8554-05a.htm" #Can stop the quest
              if npcId == ABERCROMBIE :
                 htmltext = "8555-2.htm" #Trader: second multisell
              elif npcId == SELINA :
                 htmltext = "8556-1.htm" #Buffer: buffs list
   return htmltext

 def onKill(self,npc,player,isPet):
   partyMember = self.getRandomPartyMemberState(player, STARTED)
   if not partyMember : return
   st = partyMember.getQuestState(qn)
   if st :
        if st.getState() == STARTED :
            npcId = npc.getNpcId()
            cond = st.getInt("cond")
            chance = CHANCE[npcId]*Config.RATE_DROP_QUEST
            numItems, chance = divmod(chance,MAX)
            if st.getRandom(100) <chance :
               numItems = numItems + 1
            item = 0
            if cond in [1,2] and npcId in range(1508,1513): #Splinter Stakatos
               item = CHITIN       
            elif cond==2 and npcId in range(1513,1518): #Needle Stakatos
               item = CHITIN2
            if item != 0 and numItems >= 1 :
               prevItems = st.getQuestItemsCount(item)
               if count > prevItems :
                   if count <= (prevItems + numItems) : #100 is maximum
                       numItems = count - prevItems
                       st.playSound("ItemSound.quest_middle")
                   else :
                       st.playSound("ItemSound.quest_itemget")
                   st.giveItems(item,int(numItems))
   return
           
QUEST       = Quest(628,qn,"Hunt of the Golden Ram Mercenary Force")
CREATED     = State('Start', QUEST)
STARTING    = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(KAHMAN)

QUEST.addTalkId(KAHMAN)
QUEST.addTalkId(ABERCROMBIE)
QUEST.addTalkId(SELINA)

for mob in range(1508,1518):
    QUEST.addKillId(mob)