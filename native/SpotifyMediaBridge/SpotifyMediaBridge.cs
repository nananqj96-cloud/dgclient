using System;
using System.IO;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using Windows.Foundation;
using Windows.Media.Control;
using Windows.Storage.Streams;

public static class SpotifyMediaBridge
{
	private static readonly JavaScriptSerializer Json = new JavaScriptSerializer
	{
		MaxJsonLength = 3000000
	};

	private static T Wait<T>(IAsyncOperation<T> operation)
	{
		DateTime dateTime = DateTime.UtcNow.AddSeconds(3.0);
		while (operation.Status == AsyncStatus.Started)
		{
			if (DateTime.UtcNow >= dateTime)
			{
				operation.Cancel();
				throw new TimeoutException();
			}
			Thread.Sleep(20);
		}
		try
		{
			return operation.GetResults();
		}
		finally
		{
			operation.Close();
		}
	}

	private static object Snapshot()
	{
		GlobalSystemMediaTransportControlsSessionManager globalSystemMediaTransportControlsSessionManager = Wait(GlobalSystemMediaTransportControlsSessionManager.RequestAsync());
		GlobalSystemMediaTransportControlsSession globalSystemMediaTransportControlsSession = null;
		foreach (GlobalSystemMediaTransportControlsSession session in globalSystemMediaTransportControlsSessionManager.GetSessions())
		{
			string text = session.SourceAppUserModelId ?? "";
			if ((text.Equals("Spotify.exe", StringComparison.OrdinalIgnoreCase) || text.StartsWith("SpotifyAB.SpotifyMusic_", StringComparison.OrdinalIgnoreCase)) && (globalSystemMediaTransportControlsSession == null || session.GetPlaybackInfo().PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing))
			{
				globalSystemMediaTransportControlsSession = session;
			}
		}
		if (globalSystemMediaTransportControlsSession == null)
		{
			return new
			{
				active = false
			};
		}
		GlobalSystemMediaTransportControlsSessionMediaProperties globalSystemMediaTransportControlsSessionMediaProperties = Wait(globalSystemMediaTransportControlsSession.TryGetMediaPropertiesAsync());
		string artwork = "";
		if (globalSystemMediaTransportControlsSessionMediaProperties.Thumbnail != null)
		{
			using (IRandomAccessStreamWithContentType randomAccessStreamWithContentType = Wait(globalSystemMediaTransportControlsSessionMediaProperties.Thumbnail.OpenReadAsync()))
			{
				if (randomAccessStreamWithContentType.Size != 0 && randomAccessStreamWithContentType.Size <= 1500000)
				{
					using (DataReader dataReader = new DataReader(randomAccessStreamWithContentType))
					{
						uint num = (uint)randomAccessStreamWithContentType.Size;
						uint num2 = Wait(dataReader.LoadAsync(num));
						if (num2 == num)
						{
							byte[] array = new byte[num2];
							dataReader.ReadBytes(array);
							artwork = Convert.ToBase64String(array);
						}
					}
				}
			}
		}
		GlobalSystemMediaTransportControlsSessionTimelineProperties timelineProperties = globalSystemMediaTransportControlsSession.GetTimelineProperties();
		bool flag = globalSystemMediaTransportControlsSession.GetPlaybackInfo().PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing;
		long num3 = Math.Max(0L, (long)(timelineProperties.Position - timelineProperties.StartTime).TotalMilliseconds);
		long num4 = Math.Max(0L, (long)(timelineProperties.EndTime - timelineProperties.StartTime).TotalMilliseconds);
		if (flag)
		{
			num3 += Math.Max(0L, (long)(DateTimeOffset.UtcNow - timelineProperties.LastUpdatedTime).TotalMilliseconds);
		}
		if (num4 > 0)
		{
			num3 = Math.Min(num3, num4);
		}
		return new
		{
			active = !string.IsNullOrWhiteSpace(globalSystemMediaTransportControlsSessionMediaProperties.Title),
			title = globalSystemMediaTransportControlsSessionMediaProperties.Title,
			artist = globalSystemMediaTransportControlsSessionMediaProperties.Artist,
			artwork = artwork,
			playing = flag,
			position = num3,
			duration = num4
		};
	}

	public static int Main()
	{
		StreamReader streamReader = new StreamReader(Console.OpenStandardInput(), Encoding.UTF8);
		StreamWriter streamWriter = new StreamWriter(Console.OpenStandardOutput(), new UTF8Encoding(false));
		streamWriter.AutoFlush = true;
		StreamWriter streamWriter2 = streamWriter;
		string text;
		while ((text = streamReader.ReadLine()) != null && !(text != "poll"))
		{
			try
			{
				Task<object> task = Task.Run(() => Snapshot());
				if (!task.Wait(4000))
				{
					return 2;
				}
				streamWriter2.WriteLine(Json.Serialize(task.Result));
			}
			catch
			{
				streamWriter2.WriteLine("{\"active\":false,\"error\":\"media-session-unavailable\"}");
			}
		}
		return 0;
	}
}
