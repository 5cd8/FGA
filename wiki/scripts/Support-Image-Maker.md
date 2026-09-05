## Default Images

The app comes with images of a few common Support servants and CEs like Waver, Castoria, Lunchtime, etc.

They should extract automatically after selecting the Support Directory shortly after installing FGA.
If that's not the case or some are missing, you can manually extract them in 'More options/Storage'.

## Where to put Servant/CE images?

Assuming `FGA` is the folder you chose for storage.

Under the `FGA/support/` folder, put:

- Servant images in `servant` folder
- CE images in `ce` folder
- Friend name images in `friend` folder
- Command Code images in `command_code` folder (see below)

If you want to use any servant or CE other than the default ones provided with the app, you'll have to
create their images using the `Support Image Maker` mode.

Instead of having to select multiple ascension images for a servant, you can put all the images of a servant in a folder and select the folder in
settings.

Here's what a typical directory structure looks like:

```
FGA/
-- support/
-- -- servants/
-- -- -- ozy4.png
-- -- -- melt4.png
-- -- -- Merlin/
-- -- -- -- merlin1.png
-- -- -- -- merlin2.png
-- -- -- -- merlin3.png
-- -- -- -- merlin4.png
-- -- -- -- merlin_c.png
-- -- -- Waver/
-- -- -- -- waver1.png
-- -- -- -- waver2.png
-- -- -- -- waver3.png
-- -- -- -- waver4.png
-- -- ce/
-- -- -- kscope.png
-- -- -- black_grail.png
-- -- friend/
-- -- -- mathew.png
-- -- -- recon.png
-- -- command_code/
-- -- -- np20.png
```

## Using a Command Code image to prefer one of several same-type cards

If you want the script to prefer a specific card among same-type/affinity ties (e.g. pick
the one of 2 Arts cards that has a particular Command Code attached), you need a reference
image of that Command Code's badge under `FGA/support/command_code/`.

While FGA is running on the Attack screen with the Command Code visible on a face card,
press Play, then pick **Command Code Image Maker** from the launcher dialog (next to
"Support Image Maker"). This captures the current screen live, at the same resolution FGA's
own matching uses, and opens it with a draggable box:

1. Drag the box over the badge.
2. Type a name and press **Save** (next to Done in the dialog's own button row) — you can
   save several differently named badges from the same capture before closing.
3. Save it as `FGA/support/command_code/<name>.png` (no need for `.png` when entering the
   name in settings, same rule as Servant/CE names) — the tool does this for you.
4. In the Battle Config screen, enter `<name>` under "Preferred Command Code" for the wave
   you want it to apply to (one name per line, one line per wave — same syntax as "Card
   Type Pattern").

Every Command Code has different artwork, so this only ever matches the one code whose
image you provided — it can't detect "any code is equipped" in general.

## How to use Support Image Maker?

`Support Image Maker` automatically creates images from the Support screen that can be used with the script.
You can also use it from the Friend List which is easier since you don't have to keep refreshing till the desired Servant/CE shows up.

1. Start the FGA service.
2. Open FGO. Go to support selection or friend list screen and ensure that the Servant/CE you want is visible (It is important that the complete
   Servant + CE region is visible).
3. Click on the PLAY button. A dialog shows up.
4. Click on `Support Image Maker` on the bottom-left corner of the dialog.

   Support Selection screen:

   ![Support Selection](https://i.imgur.com/ztFcyDq.png)

   Friend List screen:

   ![Friend List](https://i.imgur.com/amGZlCc.png)

5. The script will take a screenshot and show you the images it found.

   Servant & CE

   ![Servant & CE](https://i.imgur.com/VRuOlwM.png)

   CE & Friend
   ![CE & Friend](https://i.imgur.com/uRPAviO.png)

6. Check the images you want to keep.

   Type a name for the image (NO NEED for file-format like `.png`).

   For servant images, you can use a folder like: `Nero/asc1`. This will save an image named `asc1.png` in `FGA/support/servant/Nero` folder.

   By grouping in a folder, you can pick a single entry in settings to match with all ascensions and costumes.

   ![Saving](https://i.imgur.com/gBXrveP.png)

7. Click on `Done`. The selected images are saved to the correct folders.

   ![Done](https://i.imgur.com/yYpb2lH.png)
   
8. You can now use the images in your battle configs.