{-# LANGUAGE DuplicateRecordFields #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE GADTs #-}
{-# LANGUAGE NamedFieldPuns #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE StandaloneDeriving #-}
{-# LANGUAGE TypeSynonymInstances #-}
{-# OPTIONS_GHC -Wno-orphans #-}
{-# OPTIONS_GHC -fno-warn-ambiguous-fields #-}

module RandomServers where

import Data.Foldable (foldMap')
import Data.List (sortOn)
import Data.List.NonEmpty (NonEmpty)
import Data.Monoid (Sum (..))
import Simplex.Chat (defaultChatConfig, chooseRandomServers)
import Simplex.Chat.Controller (ChatConfig (..), PresetServers (..))
import Simplex.Chat.Operators
import Simplex.Messaging.Agent.Env.SQLite (ServerRoles (..))
import Simplex.Messaging.Protocol (ProtoServerWithAuth (..), SProtocolType (..), UserProtocol)
import Test.Hspec

randomServersTests :: Spec
randomServersTests = describe "choosig random servers" $ do
  it "should enable the Nome SMP server" testRandomSMPServers
  it "should enable the Nome XFTP server" testRandomXFTPServers

deriving instance Eq ServerRoles

deriving instance Eq (UserServer' s p)

testRandomSMPServers :: IO ()
testRandomSMPServers = do
  _ <- checkEnabled SPSMP 1 True =<< chooseRandomServers (presetServers defaultChatConfig)
  pure ()

testRandomXFTPServers :: IO ()
testRandomXFTPServers = do
  _ <- checkEnabled SPXFTP 1 True =<< chooseRandomServers (presetServers defaultChatConfig)
  pure ()

checkEnabled :: UserProtocol p => SProtocolType p -> Int -> Bool -> NonEmpty (PresetOperator) -> IO [NewUserServer p]
checkEnabled p n allUsed presetOps' = do
  let PresetServers {operators = presetOps} = presetServers defaultChatConfig
      presetSrvs = sortOn server' $ concatMap (pServers p) presetOps
      srvs' = sortOn server' $ concatMap (pServers p) presetOps'
      Sum toUse = foldMap' (Sum . operatorServersToUse p) presetOps
      Sum toUse' = foldMap' (Sum . operatorServersToUse p) presetOps'
  length presetOps `shouldBe` length presetOps'
  toUse `shouldBe` toUse'
  srvs' == presetSrvs `shouldBe` allUsed
  map enable srvs' `shouldBe` map enable presetSrvs
  let enbldSrvs = filter (\UserServer {enabled} -> enabled) srvs'
  toUse `shouldBe` n
  length enbldSrvs `shouldBe` n
  pure enbldSrvs
  where
    server' UserServer {server = ProtoServerWithAuth srv _} = srv
    enable :: forall p. NewUserServer p -> NewUserServer p
    enable srv = (srv :: NewUserServer p) {enabled = False}
